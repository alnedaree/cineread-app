package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.StudioAudioEngine
import com.example.data.ai.GoogleAiVoiceService
import com.example.data.ai.NewsroomAiService
import com.example.data.ai.SynthesizedAudioClip
import com.example.data.local.EpisodeArchiveRepository
import com.example.data.local.RadioDatabase
import com.example.data.local.RadioPreferencesRepository
import com.example.data.local.RadioUserSettings
import com.example.data.model.ArabicChannelRegion
import com.example.data.model.BroadcastEpisode
import com.example.data.model.BroadcastStyle
import com.example.data.model.DailyScheduleSlot
import com.example.data.model.DialogueTurn
import com.example.data.model.LiveArabicChannel
import com.example.data.model.LiveArabicChannelsCatalog
import com.example.data.model.LiveMediaMode
import com.example.data.model.NewsCategory
import com.example.data.model.RadioAnchor
import com.example.data.model.RadioCatalog
import com.example.data.model.RadioChannel
import com.example.data.model.VoiceEngineSource
import com.example.data.youtube.ResolvedYouTubeStream
import com.example.data.youtube.YouTubeLiveResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "RadioViewModel"

enum class BroadcastStage {
    STANDBY,
    FETCHING_NEWS,
    PRODUCING_SCRIPT,
    SYNTHESIZING_VOICE,
    ON_AIR,
    PAUSED,
    ERROR
}

data class RadioUiState(
    val channels: List<RadioChannel> = RadioCatalog.defaultChannels,
    val selectedChannel: RadioChannel = RadioCatalog.defaultChannels.first(),
    val currentEpisode: BroadcastEpisode? = null,
    val currentTurnIndex: Int = 0,
    val stage: BroadcastStage = BroadcastStage.STANDBY,
    val statusDetailAr: String = "",
    val errorMessageAr: String? = null,
    val activeVoiceSource: VoiceEngineSource = VoiceEngineSource.GOOGLE_AI_VOICE,
    val voiceFallbackNoticeAr: String? = null,
    val settings: RadioUserSettings = RadioUserSettings(),
    val archivedEpisodes: List<BroadcastEpisode> = emptyList(),
    val dailySchedule: List<DailyScheduleSlot> = RadioCatalog.buildDailySchedule(),
    val testingAnchorId: String? = null,
    val voiceTestErrorAr: String? = null,
    val settingsLoaded: Boolean = false,
    val liveArabicChannels: List<LiveArabicChannel> = LiveArabicChannelsCatalog.officialChannels,
    val selectedLiveArabicChannel: LiveArabicChannel = LiveArabicChannelsCatalog.officialChannels.first(),
    val resolvedLiveStream: ResolvedYouTubeStream? = null,
    val isResolvingLiveStream: Boolean = false,
    val liveStreamVersion: Int = 0
) {
    val selectedCategories: List<NewsCategory>
        get() = RadioCatalog.categoriesByIds(settings.selectedCategoryIds)

    val currentTurn: DialogueTurn?
        get() = currentEpisode?.turns?.getOrNull(currentTurnIndex)

    val currentAnchor: RadioAnchor
        get() = currentTurn?.let { RadioCatalog.anchorById(it.anchorId) }
            ?: RadioCatalog.anchorById(selectedChannel.anchorIds.firstOrNull() ?: "tariq")

    val nextTurn: DialogueTurn?
        get() = currentEpisode?.turns?.getOrNull(currentTurnIndex + 1)

    val isBusyGenerating: Boolean
        get() = stage == BroadcastStage.FETCHING_NEWS ||
            stage == BroadcastStage.PRODUCING_SCRIPT ||
            stage == BroadcastStage.SYNTHESIZING_VOICE

    val isPlayingOrLive: Boolean
        get() = stage == BroadcastStage.ON_AIR || stage == BroadcastStage.SYNTHESIZING_VOICE
}

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = RadioPreferencesRepository(application)
    private val archiveRepo = EpisodeArchiveRepository(RadioDatabase.getInstance(application).episodeDao())
    private val newsroomAi = NewsroomAiService()
    private val voiceAi = GoogleAiVoiceService()
    private val audioEngine = StudioAudioEngine(application)

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    val spectrumLevels: StateFlow<List<Float>> = audioEngine.spectrumLevels

    private val audioCache = ConcurrentHashMap<String, SynthesizedAudioClip>()
    private val inFlightPrefetches = ConcurrentHashMap<String, Deferred<SynthesizedAudioClip?>>()

    private var broadcastJob: Job? = null
    private var voiceTestJob: Job? = null
    private var liveResolveJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                prefsRepo.settingsFlow,
                archiveRepo.observeEpisodes()
            ) { settings, episodes ->
                Pair(settings, episodes)
            }.collect { (settings, episodes) ->
                audioEngine.setPlaybackSpeed(settings.playbackSpeed)
                val allChannels = RadioCatalog.defaultChannels + settings.customChannels
                val resolvedSelected = allChannels.firstOrNull { it.id == settings.selectedChannelId }
                    ?: allChannels.first()
                val allLiveArabic = LiveArabicChannelsCatalog.officialChannels + settings.customLiveArabicChannels

                _uiState.update { prev ->
                    val currentLiveStillExists = allLiveArabic.firstOrNull {
                        it.id == prev.selectedLiveArabicChannel.id
                    } ?: allLiveArabic.first()
                    prev.copy(
                        channels = allChannels,
                        selectedChannel = resolvedSelected,
                        settings = settings,
                        archivedEpisodes = episodes,
                        dailySchedule = RadioCatalog.buildDailySchedule(),
                        settingsLoaded = true,
                        liveArabicChannels = allLiveArabic,
                        selectedLiveArabicChannel = currentLiveStillExists
                    )
                }
            }
        }
    }

    fun selectChannel(channel: RadioChannel, autoStartIfLive: Boolean = true) {
        val wasActive = _uiState.value.isPlayingOrLive || _uiState.value.stage == BroadcastStage.PAUSED
        val isSame = _uiState.value.selectedChannel.id == channel.id

        viewModelScope.launch {
            prefsRepo.setSelectedChannel(channel.id)
        }

        if (isSame) {
            if (_uiState.value.stage == BroadcastStage.STANDBY || _uiState.value.stage == BroadcastStage.ERROR) {
                startLiveBroadcast(channelOverride = channel, isBreakingFlash = false)
            }
            return
        }

        stopBroadcastInternal()
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                currentEpisode = null,
                currentTurnIndex = 0,
                stage = BroadcastStage.STANDBY,
                errorMessageAr = null,
                voiceFallbackNoticeAr = null
            )
        }

        if (autoStartIfLive && wasActive) {
            startLiveBroadcast(channelOverride = channel, isBreakingFlash = false)
        }
    }

    fun tuneAndBroadcastChannel(channel: RadioChannel) {
        viewModelScope.launch {
            prefsRepo.setSelectedChannel(channel.id)
        }
        stopBroadcastInternal()
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                errorMessageAr = null,
                voiceFallbackNoticeAr = null
            )
        }
        startLiveBroadcast(channelOverride = channel, isBreakingFlash = false)
    }

    fun togglePlayPause() {
        val state = _uiState.value
        when (state.stage) {
            BroadcastStage.ON_AIR -> {
                audioEngine.pause()
                _uiState.update { it.copy(stage = BroadcastStage.PAUSED) }
            }
            BroadcastStage.PAUSED -> {
                val episode = state.currentEpisode
                if (episode != null && broadcastJob?.isActive == true) {
                    audioEngine.resume()
                    _uiState.update { it.copy(stage = BroadcastStage.ON_AIR) }
                } else if (episode != null) {
                    playEpisodeFromTurn(episode, state.currentTurnIndex)
                } else {
                    startLiveBroadcast(isBreakingFlash = false)
                }
            }
            BroadcastStage.STANDBY, BroadcastStage.ERROR -> {
                val existing = state.currentEpisode
                if (existing != null && state.stage == BroadcastStage.STANDBY &&
                    existing.channelId == state.selectedChannel.id &&
                    state.currentTurnIndex < existing.turns.size
                ) {
                    playEpisodeFromTurn(existing, state.currentTurnIndex)
                } else {
                    startLiveBroadcast(isBreakingFlash = false)
                }
            }
            BroadcastStage.FETCHING_NEWS,
            BroadcastStage.PRODUCING_SCRIPT,
            BroadcastStage.SYNTHESIZING_VOICE -> {
                // Allow cancelling an in-progress generation cleanly
                stopBroadcastInternal()
                _uiState.update { it.copy(stage = BroadcastStage.STANDBY, statusDetailAr = "") }
            }
        }
    }

    fun triggerBreakingFlash() {
        startLiveBroadcast(isBreakingFlash = true)
    }

    fun startLiveBroadcast(
        channelOverride: RadioChannel? = null,
        isBreakingFlash: Boolean = false
    ) {
        stopBroadcastInternal()
        val channel = channelOverride ?: _uiState.value.selectedChannel
        val settings = _uiState.value.settings
        val previousHeadlines = _uiState.value.currentEpisode?.breakingTickerHeadlines.orEmpty()

        broadcastJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        selectedChannel = channel,
                        stage = BroadcastStage.FETCHING_NEWS,
                        statusDetailAr = if (isBreakingFlash) {
                            "جاري رصد آخر الأخبار العاجلة الآن لـ ${channel.nameAr}…"
                        } else {
                            "جاري متابعة أخبار اليوم وتقارير القنوات لـ ${channel.nameAr}…"
                        },
                        errorMessageAr = null,
                        voiceFallbackNoticeAr = null
                    )
                }

                val activeCategories = RadioCatalog.categoriesByIds(settings.selectedCategoryIds)
                val gathered = newsroomAi.gatherLiveNews(
                    channel = channel,
                    selectedCategories = activeCategories,
                    isBreakingFlash = isBreakingFlash,
                    previousHeadlines = previousHeadlines
                )

                _uiState.update {
                    it.copy(
                        stage = BroadcastStage.PRODUCING_SCRIPT,
                        statusDetailAr = "رئيس التحرير الذكي يرتّب فقرات البرنامج ويوزعها على المذيعين…"
                    )
                }

                val episode = newsroomAi.produceBroadcastEpisode(
                    channel = channel,
                    selectedCategories = activeCategories,
                    style = settings.broadcastStyle,
                    newsBrief = gathered,
                    isBreakingFlash = isBreakingFlash
                )

                // Archive the freshly produced episode in local Room cache
                launch(Dispatchers.IO) {
                    try {
                        archiveRepo.saveEpisode(episode)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed saving episode to archive", e)
                    }
                }

                _uiState.update {
                    it.copy(
                        currentEpisode = episode,
                        currentTurnIndex = 0,
                        dailySchedule = RadioCatalog.buildDailySchedule()
                    )
                }

                if (settings.studioChimeEnabled) {
                    audioEngine.playBroadcastChime()
                }

                runEpisodePlaybackLoop(episode, startIndex = 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live broadcast pipeline failed at stage=${_uiState.value.stage}", e)
                _uiState.update {
                    it.copy(
                        stage = BroadcastStage.ERROR,
                        statusDetailAr = "",
                        errorMessageAr = formatFriendlyArabicError(_uiState.value.stage, e)
                    )
                }
            }
        }
    }

    fun playArchivedEpisode(episode: BroadcastEpisode) {
        val matchingChannel = _uiState.value.channels.firstOrNull { it.id == episode.channelId }
            ?: _uiState.value.selectedChannel
        stopBroadcastInternal()
        _uiState.update {
            it.copy(
                selectedChannel = matchingChannel,
                currentEpisode = episode,
                currentTurnIndex = 0,
                errorMessageAr = null,
                voiceFallbackNoticeAr = null
            )
        }
        playEpisodeFromTurn(episode, 0)
    }

    fun jumpToTurn(turnIndex: Int) {
        val episode = _uiState.value.currentEpisode ?: return
        if (turnIndex !in episode.turns.indices) return
        playEpisodeFromTurn(episode, turnIndex)
    }

    fun nextTurn() {
        val state = _uiState.value
        val episode = state.currentEpisode ?: return
        val next = state.currentTurnIndex + 1
        if (next < episode.turns.size) {
            playEpisodeFromTurn(episode, next)
        } else if (state.settings.autoContinuousBroadcast) {
            startLiveBroadcast(isBreakingFlash = false)
        }
    }

    fun previousTurn() {
        val state = _uiState.value
        val episode = state.currentEpisode ?: return
        val prev = (state.currentTurnIndex - 1).coerceAtLeast(0)
        playEpisodeFromTurn(episode, prev)
    }

    private fun playEpisodeFromTurn(episode: BroadcastEpisode, startIndex: Int) {
        stopBroadcastInternal()
        broadcastJob = viewModelScope.launch {
            try {
                runEpisodePlaybackLoop(episode, startIndex)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Episode playback failed", e)
                _uiState.update {
                    it.copy(
                        stage = BroadcastStage.ERROR,
                        errorMessageAr = e.localizedMessage ?: "حدث خطأ أثناء تشغيل البث الصوتي."
                    )
                }
            }
        }
    }

    private suspend fun runEpisodePlaybackLoop(episode: BroadcastEpisode, startIndex: Int) {
        var idx = startIndex
        while (idx < episode.turns.size) {
            val turn = episode.turns[idx]
            val anchor = RadioCatalog.anchorById(turn.anchorId)

            _uiState.update {
                it.copy(
                    currentEpisode = episode,
                    currentTurnIndex = idx,
                    stage = BroadcastStage.SYNTHESIZING_VOICE,
                    statusDetailAr = "تجهيز صوت ${anchor.nameAr} عبر Google AI Voice…"
                )
            }

            // Synthesize turns sequentially (no burst parallel prefetch) so Free Tier RPM stays smooth
            var clip: SynthesizedAudioClip? = audioCache[turn.id]
            var voiceError: Exception? = null

            if (clip == null) {
                try {
                    clip = voiceAi.synthesizeBroadcastLine(
                        anchor = anchor,
                        arabicText = turn.textAr,
                        emotionCue = turn.emotionCue
                    )
                    audioCache[turn.id] = clip
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Google AI Voice error on turn ${turn.id}", e)
                    voiceError = e
                }
            }

            if (clip != null) {
                _uiState.update {
                    it.copy(
                        stage = BroadcastStage.ON_AIR,
                        statusDetailAr = "${anchor.nameAr} على الهواء الآن",
                        activeVoiceSource = VoiceEngineSource.GOOGLE_AI_VOICE,
                        voiceFallbackNoticeAr = null
                    )
                }
                audioEngine.playPcmClip(clip)
            } else if (_uiState.value.settings.allowDeviceTtsFallback) {
                _uiState.update {
                    it.copy(
                        stage = BroadcastStage.ON_AIR,
                        statusDetailAr = "${anchor.nameAr} (البديل الصوتي المحلي)",
                        activeVoiceSource = VoiceEngineSource.DEVICE_TTS_FALLBACK,
                        voiceFallbackNoticeAr = "تم تجاوز الحد المجاني المؤقت لصوت Google AI Voice — يعمل الصوت المحلي لاستمرار البث."
                    )
                }
                val spoke = audioEngine.speakWithDeviceFallback(anchor, turn.textAr)
                if (!spoke) {
                    throw voiceError ?: IllegalStateException("تعذر تشغيل الصوت لهذه الفقرة.")
                }
            } else {
                throw voiceError ?: IllegalStateException("تعذر توليد الصوت عبر Google AI Voice.")
            }

            idx++
            if (idx < episode.turns.size) {
                delay(350)
            }
        }

        // Episode finished! If 24/7 Auto-Continuous mode is ON, wait politely before producing the next bulletin
        if (_uiState.value.settings.autoContinuousBroadcast) {
            _uiState.update {
                it.copy(
                    stage = BroadcastStage.STANDBY,
                    statusDetailAr = "استراحة إذاعية قصيرة قبل النشرة التالية…"
                )
            }
            delay(8_000)
            startLiveBroadcast(
                channelOverride = _uiState.value.selectedChannel,
                isBreakingFlash = false
            )
        } else {
            _uiState.update {
                it.copy(
                    stage = BroadcastStage.STANDBY,
                    statusDetailAr = "انتهت النشرة الإخبارية — اضغط للتحديث والبدء من جديد"
                )
            }
        }
    }

    private fun formatFriendlyArabicError(stage: BroadcastStage, e: Exception): String {
        val raw = e.localizedMessage.orEmpty()
        val isQuota = raw.contains("quota", ignoreCase = true) ||
            raw.contains("rate limit", ignoreCase = true) ||
            raw.contains("rate-limits", ignoreCase = true) ||
            raw.contains("429", ignoreCase = true) ||
            raw.contains("resource_exhausted", ignoreCase = true)

        if (isQuota) {
            val retryMatch = Regex("retry in ([0-9]+(?:\\.[0-9]+)?)s", RegexOption.IGNORE_CASE).find(raw)
            val waitSeconds = retryMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt()?.coerceAtLeast(5)
            return if (waitSeconds != null) {
                "تم الوصول للحد المجاني المؤقت لطلبات الذكاء الاصطناعي. انتظر حوالي $waitSeconds ثانية ثم اضغط «إعادة المحاولة»، أو شغّل نشرة محفوظة من الأرشيف."
            } else {
                "تم الوصول للحد المجاني المؤقت لطلبات الذكاء الاصطناعي في هذه الدقيقة. انتظر لحظات ثم اضغط «إعادة المحاولة»."
            }
        }

        val stagePrefix = when (stage) {
            BroadcastStage.FETCHING_NEWS -> "تعذر رصد الأخبار: "
            BroadcastStage.PRODUCING_SCRIPT -> "تعذر إعداد نص البرنامج: "
            BroadcastStage.SYNTHESIZING_VOICE -> "تعذر تشغيل Google AI Voice: "
            else -> "تعذر بدء البث: "
        }
        val cleanDetail = raw.lineSequence().firstOrNull()?.take(140)?.takeIf { it.isNotBlank() }
            ?: "تحقق من اتصال الإنترنت ثم أعد المحاولة."
        return "$stagePrefix$cleanDetail"
    }

    private fun prefetchTurnAudio(turn: DialogueTurn) {
        if (audioCache.containsKey(turn.id) || inFlightPrefetches.containsKey(turn.id)) return
        val anchor = RadioCatalog.anchorById(turn.anchorId)
        val deferred = viewModelScope.async(Dispatchers.IO) {
            try {
                val clip = voiceAi.synthesizeBroadcastLine(
                    anchor = anchor,
                    arabicText = turn.textAr,
                    emotionCue = turn.emotionCue
                )
                audioCache[turn.id] = clip
                clip
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Prefetch failed for ${turn.id}: ${e.localizedMessage}")
                null
            } finally {
                inFlightPrefetches.remove(turn.id)
            }
        }
        inFlightPrefetches[turn.id] = deferred
    }

    fun testAnchorVoice(anchor: RadioAnchor) {
        voiceTestJob?.cancel()
        if (_uiState.value.stage == BroadcastStage.ON_AIR) {
            audioEngine.pause()
            _uiState.update { it.copy(stage = BroadcastStage.PAUSED) }
        }
        voiceTestJob = viewModelScope.launch {
            _uiState.update {
                it.copy(testingAnchorId = anchor.id, voiceTestErrorAr = null)
            }
            val sampleLine = when (anchor.id) {
                "tariq" -> "أهلاً بكم مستمعينا في شبكة أثير الأخبار، معكم طارق المنصور في تغطية حية ومباشرة على مدار الساعة."
                "layla" -> "مرحباً بكم، أنا ليلى العتيبي، نرافقكم في حوارات وتقارير خاصة تقربكم من قلب الحدث."
                else -> "تحية طيبة، معكم كريم الشامي لتحليل أحدث مؤشرات الأسواق والتقنية والذكاء الاصطناعي اليوم."
            }
            try {
                val clip = voiceAi.synthesizeBroadcastLine(
                    anchor = anchor,
                    arabicText = sampleLine,
                    emotionCue = "ترحيبي إذاعي واضح"
                )
                audioEngine.playPcmClip(clip)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Anchor voice test failed for ${anchor.id}", e)
                _uiState.update {
                    it.copy(
                        voiceTestErrorAr = e.localizedMessage
                            ?: "تعذر اختبار صوت Google AI Voice حالياً."
                    )
                }
            } finally {
                _uiState.update { it.copy(testingAnchorId = null) }
            }
        }
    }

    fun createCustomChannel(
        nameAr: String,
        topicPrompt: String,
        selectedAnchorIds: List<String>
    ) {
        val cleanName = nameAr.trim()
        val cleanTopic = topicPrompt.trim()
        if (cleanName.isBlank() || cleanTopic.isBlank()) return

        val freqVal = (88.1 + ((cleanName.hashCode().and(0x7FFF)) % 190) / 10.0)
        val channel = RadioChannel(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            nameAr = cleanName,
            frequencyLabel = String.format(java.util.Locale.US, "%.1f FM", freqVal),
            taglineAr = cleanTopic,
            categoryAr = "قناة مخصصة",
            topicPrompt = cleanTopic,
            anchorIds = selectedAnchorIds.ifEmpty { listOf("tariq", "layla") },
            accentIndex = (cleanName.length % 5),
            isCustom = true
        )
        viewModelScope.launch {
            prefsRepo.addCustomChannel(channel)
            tuneAndBroadcastChannel(channel)
        }
    }

    fun deleteCustomChannel(channelId: String) {
        viewModelScope.launch {
            if (_uiState.value.selectedChannel.id == channelId) {
                stopBroadcastInternal()
            }
            prefsRepo.removeCustomChannel(channelId)
        }
    }

    fun deleteArchivedEpisode(episodeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            archiveRepo.deleteEpisode(episodeId)
        }
    }

    fun updateBroadcastStyle(style: BroadcastStyle) {
        viewModelScope.launch { prefsRepo.setBroadcastStyle(style) }
    }

    fun toggleAutoContinuous(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoContinuous(enabled) }
    }

    fun toggleStudioChime(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setStudioChime(enabled) }
    }

    fun toggleDeviceTtsFallback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAllowDeviceFallback(enabled) }
    }

    fun toggleDarkStudioTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkStudioTheme(enabled) }
    }

    fun saveSelectedCategories(categoryIds: Set<String>, startBroadcastNow: Boolean) {
        if (categoryIds.isEmpty()) return
        viewModelScope.launch {
            prefsRepo.setSelectedCategories(categoryIds)
            val mainChannel = _uiState.value.channels.firstOrNull { it.id == "breaking_general" }
                ?: _uiState.value.selectedChannel
            if (startBroadcastNow) {
                prefsRepo.setSelectedChannel(mainChannel.id)
                _uiState.update { prev ->
                    prev.copy(
                        selectedChannel = mainChannel,
                        settings = prev.settings.copy(
                            selectedCategoryIds = categoryIds,
                            hasConfiguredCategories = true,
                            selectedChannelId = mainChannel.id
                        )
                    )
                }
                startLiveBroadcast(channelOverride = mainChannel, isBreakingFlash = false)
            }
        }
    }

    fun dismissCategoryOnboarding() {
        viewModelScope.launch {
            prefsRepo.markCategoriesConfigured()
        }
    }

    fun cyclePlaybackSpeed() {
        val current = _uiState.value.settings.playbackSpeed
        val next = when {
            current < 1.1f -> 1.15f
            current < 1.25f -> 1.3f
            else -> 1.0f
        }
        viewModelScope.launch { prefsRepo.setPlaybackSpeed(next) }
    }

    // -------------------------------------------------------------------------
    // Live Arabic News Channels (YouTube Live — Video & Radio Audio-Only Modes)
    // -------------------------------------------------------------------------

    fun pauseAiBroadcastForExternalLive() {
        val state = _uiState.value
        if (state.stage == BroadcastStage.ON_AIR) {
            audioEngine.pause()
            _uiState.update { it.copy(stage = BroadcastStage.PAUSED) }
        } else if (state.isBusyGenerating) {
            stopBroadcastInternal()
            _uiState.update { it.copy(stage = BroadcastStage.STANDBY, statusDetailAr = "") }
        }
        voiceTestJob?.cancel()
    }

    fun ensureLiveArabicChannelResolved() {
        pauseAiBroadcastForExternalLive()
        val state = _uiState.value
        if (state.resolvedLiveStream == null && !state.isResolvingLiveStream) {
            selectLiveArabicChannel(state.selectedLiveArabicChannel)
        }
    }

    fun selectLiveArabicChannel(channel: LiveArabicChannel) {
        pauseAiBroadcastForExternalLive()
        liveResolveJob?.cancel()
        _uiState.update {
            it.copy(
                selectedLiveArabicChannel = channel,
                isResolvingLiveStream = true
            )
        }
        liveResolveJob = viewModelScope.launch {
            val resolved = YouTubeLiveResolver.resolveLiveStream(channel)
            _uiState.update { prev ->
                if (prev.selectedLiveArabicChannel.id != channel.id) prev
                else prev.copy(
                    resolvedLiveStream = resolved,
                    isResolvingLiveStream = false,
                    liveStreamVersion = prev.liveStreamVersion + 1
                )
            }
        }
    }

    fun reloadLiveArabicStream() {
        selectLiveArabicChannel(_uiState.value.selectedLiveArabicChannel)
    }

    fun selectNextLiveArabicChannel() {
        val list = _uiState.value.liveArabicChannels
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _uiState.value.selectedLiveArabicChannel.id }
        val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % list.size
        selectLiveArabicChannel(list[nextIdx])
    }

    fun selectPreviousLiveArabicChannel() {
        val list = _uiState.value.liveArabicChannels
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _uiState.value.selectedLiveArabicChannel.id }
        val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
        selectLiveArabicChannel(list[prevIdx])
    }

    fun setLiveMediaMode(mode: LiveMediaMode) {
        viewModelScope.launch {
            prefsRepo.setLiveMediaMode(mode)
        }
    }

    fun addCustomLiveArabicChannel(nameAr: String, linkOrHandle: String) {
        val cleanName = nameAr.trim()
        val cleanInput = linkOrHandle.trim()
        if (cleanName.isBlank() || cleanInput.isBlank()) return

        val (parsedHandleOrUrl, parsedVideoId) = YouTubeLiveResolver.parseUserInput(cleanInput)
        val channelIdMatch = Regex("(UC[A-Za-z0-9_-]{20,})").find(cleanInput)?.groupValues?.getOrNull(1)
        val shortBadge = cleanName
            .replace("قناة", "")
            .trim()
            .split(" ")
            .firstOrNull()
            ?.take(8)
            ?.ifBlank { "مباشر" } ?: "مباشر"

        val customChannel = LiveArabicChannel(
            id = "yt_custom_${UUID.randomUUID().toString().take(8)}",
            nameAr = cleanName,
            shortBadgeAr = shortBadge,
            taglineAr = "بث مباشر مخصص عبر يوتيوب — صوت وفيديو",
            countryAr = "قناة مخصصة",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = parsedHandleOrUrl,
            knownChannelId = channelIdMatch,
            customVideoId = parsedVideoId,
            accentIndex = cleanName.length % 5,
            isCustom = true
        )
        viewModelScope.launch {
            prefsRepo.addCustomLiveArabicChannel(customChannel)
            selectLiveArabicChannel(customChannel)
        }
    }

    fun removeCustomLiveArabicChannel(channelId: String) {
        val wasSelected = _uiState.value.selectedLiveArabicChannel.id == channelId
        viewModelScope.launch {
            prefsRepo.removeCustomLiveArabicChannel(channelId)
            if (wasSelected) {
                selectLiveArabicChannel(LiveArabicChannelsCatalog.officialChannels.first())
            }
        }
    }

    private fun stopBroadcastInternal() {
        broadcastJob?.cancel()
        broadcastJob = null
        inFlightPrefetches.values.forEach { it.cancel() }
        inFlightPrefetches.clear()
        audioEngine.stopCurrentAudio()
    }

    override fun onCleared() {
        super.onCleared()
        stopBroadcastInternal()
        voiceTestJob?.cancel()
        liveResolveJob?.cancel()
        audioEngine.release()
    }
}
