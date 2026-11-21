package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.ArabicChannelRegion
import com.example.data.model.LiveArabicChannel
import com.example.data.model.LiveMediaMode
import com.example.data.youtube.ResolvedYouTubeStream
import com.example.data.youtube.YouTubeLiveResolver
import com.example.ui.theme.BroadcastGold
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.VoiceSignalCyan
import com.example.viewmodel.RadioUiState
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveArabicTvScreen(
    uiState: RadioUiState,
    onScreenEntered: () -> Unit,
    onSelectChannel: (LiveArabicChannel) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onReloadStream: () -> Unit,
    onSetMediaMode: (LiveMediaMode) -> Unit,
    onAddCustomChannel: (nameAr: String, linkOrHandle: String) -> Unit,
    onRemoveCustomChannel: (channelId: String) -> Unit
) {
    val context = LocalContext.current
    val mediaMode = uiState.settings.liveMediaMode
    val selectedChannel = uiState.selectedLiveArabicChannel
    val resolvedStream = uiState.resolvedLiveStream

    var selectedRegion by rememberSaveable { mutableStateOf(ArabicChannelRegion.ALL) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var isStreamPlaying by remember { mutableStateOf(true) }
    var isStreamMuted by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Pause AI radio and resolve the active channel on first entry
    LaunchedEffect(Unit) {
        onScreenEntered()
    }

    // Reset local play/mute UI state whenever a fresh live stream version loads
    LaunchedEffect(uiState.liveStreamVersion) {
        if (uiState.liveStreamVersion > 0) {
            isStreamPlaying = true
            isStreamMuted = false
        }
    }

    // Cleanly stop WebView playback when leaving this tab so audio never leaks
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.onPause()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
            webViewRef = null
        }
    }

    val filteredChannels = remember(uiState.liveArabicChannels, selectedRegion) {
        if (selectedRegion == ArabicChannelRegion.ALL) {
            uiState.liveArabicChannels
        } else {
            uiState.liveArabicChannels.filter { it.region == selectedRegion }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Live Badge & Add Custom YouTube Channel Action
            item(key = "header") {
                LiveTvTopHeader(
                    onAddClick = { showAddDialog = true }
                )
            }

            // 2. Dual Mode Switcher: Video vs Radio Audio-Only
            item(key = "mode_switcher") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = mediaMode == LiveMediaMode.VIDEO,
                        onClick = { onSetMediaMode(LiveMediaMode.VIDEO) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_video_button")
                    ) {
                        Text(
                            text = stringResource(R.string.mode_video),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    SegmentedButton(
                        selected = mediaMode == LiveMediaMode.RADIO_AUDIO,
                        onClick = { onSetMediaMode(LiveMediaMode.RADIO_AUDIO) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_radio_audio_button")
                    ) {
                        Text(
                            text = stringResource(R.string.mode_radio_audio),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Unified Seamless Dual-Mode Player Card (Single Persistent WebView!)
            item(key = "unified_player") {
                UnifiedYouTubeLivePlayerCard(
                    channel = selectedChannel,
                    mediaMode = mediaMode,
                    resolvedStream = resolvedStream,
                    streamVersion = uiState.liveStreamVersion,
                    isResolving = uiState.isResolvingLiveStream,
                    isPlaying = isStreamPlaying,
                    isMuted = isStreamMuted,
                    onWebViewReady = { webViewRef = it },
                    onTogglePlayPause = {
                        val nextPlay = !isStreamPlaying
                        isStreamPlaying = nextPlay
                        val js = if (nextPlay) "window.atheerPlay && window.atheerPlay();" else "window.atheerPause && window.atheerPause();"
                        webViewRef?.evaluateJavascript(js, null)
                    },
                    onToggleMute = {
                        val nextMute = !isStreamMuted
                        isStreamMuted = nextMute
                        val js = if (nextMute) "window.atheerMute && window.atheerMute();" else "window.atheerUnmute && window.atheerUnmute();"
                        webViewRef?.evaluateJavascript(js, null)
                    },
                    onNextChannel = onNextChannel,
                    onPreviousChannel = onPreviousChannel,
                    onReload = onReloadStream,
                    onSwitchMode = { targetMode -> onSetMediaMode(targetMode) },
                    onOpenExternalYouTube = {
                        val url = resolvedStream?.videoId?.let {
                            "https://www.youtube.com/watch?v=$it"
                        } ?: selectedChannel.youtubeLivePageUrl
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
            }

            // 4. Region Filter Chips
            item(key = "region_filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ArabicChannelRegion.entries, key = { it.name }) { region ->
                        val selected = selectedRegion == region
                        FilterChip(
                            selected = selected,
                            onClick = { selectedRegion = region },
                            label = {
                                Text(
                                    text = region.labelAr,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("region_filter_${region.name.lowercase()}")
                        )
                    }
                }
            }

            // 5. Section Title
            item(key = "channels_count_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة القنوات الإخبارية (${filteredChannels.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (mediaMode == LiveMediaMode.RADIO_AUDIO) "استماع إذاعي" else "بث مرئي",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mediaMode == LiveMediaMode.RADIO_AUDIO) BroadcastGold else VoiceSignalCyan
                    )
                }
            }

            // 6. Arabic News Channels List
            items(filteredChannels, key = { it.id }) { channel ->
                LiveArabicChannelCard(
                    channel = channel,
                    isSelected = channel.id == selectedChannel.id,
                    mediaMode = mediaMode,
                    onSelect = { onSelectChannel(channel) },
                    onDeleteCustom = if (channel.isCustom) {
                        { onRemoveCustomChannel(channel.id) }
                    } else null
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        AddCustomYouTubeChannelDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, link ->
                onAddCustomChannel(name, link)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LiveTvTopHeader(
    onAddClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_tally_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tally_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = OnAirCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(dotAlpha)
                                .clip(CircleShape)
                                .background(OnAirCrimson)
                        )
                        Text(
                            text = "مباشر • LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnAirCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.title_live_tv),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.subtitle_live_tv),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalIconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("add_yt_channel_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.action_add_yt_channel)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun UnifiedYouTubeLivePlayerCard(
    channel: LiveArabicChannel,
    mediaMode: LiveMediaMode,
    resolvedStream: ResolvedYouTubeStream?,
    streamVersion: Int,
    isResolving: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    onWebViewReady: (WebView) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onReload: () -> Unit,
    onSwitchMode: (LiveMediaMode) -> Unit,
    onOpenExternalYouTube: () -> Unit
) {
    val accentColor = channelAccentColor(channel.accentIndex)
    var lastLoadedVersion by remember { mutableIntStateOf(-1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (mediaMode == LiveMediaMode.RADIO_AUDIO) {
                BroadcastGold.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Player viewport: Single persistent WebView kept alive across Video <-> Radio toggles!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (mediaMode == LiveMediaMode.VIDEO) {
                            Modifier.aspectRatio(16f / 9f)
                        } else {
                            Modifier.height(268.dp)
                        }
                    )
                    .background(Color(0xFF0A0F1C))
            ) {
                // Keep the WebView ALWAYS attached and active so audio plays continuously when switching to Radio mode
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.parseColor("#0A0F1C"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadsImagesAutomatically = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    // Keep YouTube embed & mobile pages inside the WebView
                                    return !(url.contains("youtube.com") || url.contains("youtu.be") || url.contains("googlevideo.com"))
                                }
                            }
                            onWebViewReady(this)
                        }
                    },
                    update = { webView ->
                        onWebViewReady(webView)
                        if (resolvedStream != null && streamVersion != lastLoadedVersion) {
                            lastLoadedVersion = streamVersion
                            if (!resolvedStream.videoId.isNullOrBlank() || !resolvedStream.channelId.isNullOrBlank()) {
                                val html = YouTubeLiveResolver.buildPlayerHtml(resolvedStream)
                                webView.loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            } else {
                                webView.loadUrl(resolvedStream.fallbackDirectUrl)
                            }
                        }
                    },
                    modifier = if (mediaMode == LiveMediaMode.VIDEO) {
                        Modifier.fillMaxSize()
                    } else {
                        // Active 1.dp anchor behind Radio Console so WebView keeps streaming audio without unloading
                        Modifier
                            .size(1.dp)
                            .align(Alignment.BottomEnd)
                    }
                )

                // Radio Audio-Only Console Overlay (covers the Box seamlessly while WebView audio plays)
                if (mediaMode == LiveMediaMode.RADIO_AUDIO) {
                    RadioAudioConsoleOverlay(
                        channel = channel,
                        accentColor = accentColor,
                        isResolving = isResolving,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        onTogglePlayPause = onTogglePlayPause,
                        onToggleMute = onToggleMute,
                        onNextChannel = onNextChannel,
                        onPreviousChannel = onPreviousChannel,
                        onSwitchToVideo = { onSwitchMode(LiveMediaMode.VIDEO) }
                    )
                }

                // Loading Indicator overlay when resolving a new channel in Video mode
                if (isResolving && mediaMode == LiveMediaMode.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC0A0F1C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = OnAirCrimson,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "جاري الاتصال بالبث المباشر لـ ${channel.nameAr}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }

            // Player Footer Bar: Channel Info + Quick Utility Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = channel.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = channel.countryAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (mediaMode == LiveMediaMode.RADIO_AUDIO) {
                                stringResource(R.string.label_radio_mode_active)
                            } else {
                                channel.taglineAr
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReload,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("reload_yt_stream_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_reload_stream),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenExternalYouTube,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("open_external_youtube_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_open_youtube),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (mediaMode == LiveMediaMode.VIDEO) {
                        FilledTonalIconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("video_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = if (isMuted) "إلغاء الكتم" else "كتم الصوت"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioAudioConsoleOverlay(
    channel: LiveArabicChannel,
    accentColor: Color,
    isResolving: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onSwitchToVideo: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "radio_console_anim")
    val ringScale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val isActivelyAudible = isPlaying && !isMuted && !isResolving

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1629),
                        Color(0xFF141E36),
                        Color(0xFF0B1120)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar inside Radio Console
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = BroadcastGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, BroadcastGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = BroadcastGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "إذاعة مباشرة • صوت فقط",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = onSwitchToVideo,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("switch_to_video_quick_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = VoiceSignalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "عرض الفيديو",
                        style = MaterialTheme.typography.labelMedium,
                        color = VoiceSignalCyan
                    )
                }
            }

            // Center Channel Emblem + Animated Spectrum Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pulsing Channel Emblem
                Box(
                    modifier = Modifier.size(78.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActivelyAudible) {
                        Box(
                            modifier = Modifier
                                .size((74 * ringScale).dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                        )
                    }
                    Surface(
                        modifier = Modifier.size(66.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.22f),
                        border = BorderStroke(2.dp, accentColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Text(
                                    text = channel.shortBadgeAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isResolving -> "جاري توليف موجة البث المباشر…"
                            isMuted -> "الصوت مكتوم — اضغط لإعادة الصوت"
                            isPlaying -> "البث الإذاعي الحي يعمل الآن"
                            else -> "البث متوقف مؤقتاً"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActivelyAudible) StudioEmerald else Color(0xFFCBD5E1)
                    )

                    // Animated Radio Audio Equalizer Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        val barCount = 24
                        val gap = 4.dp.toPx()
                        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(3f)
                        for (i in 0 until barCount) {
                            val norm = if (isActivelyAudible) {
                                val wave1 = sin(wavePhase + i * 0.55f)
                                val wave2 = sin(wavePhase * 1.7f + i * 0.9f)
                                ((wave1 * 0.5f + wave2 * 0.5f + 1f) / 2f).coerceIn(0.18f, 1f)
                            } else {
                                0.15f
                            }
                            val barH = size.height * norm
                            val x = i * (barWidth + gap)
                            val y = (size.height - barH) / 2f
                            drawRoundRect(
                                color = if (i % 3 == 0) accentColor else VoiceSignalCyan.copy(alpha = 0.85f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }

            // Bottom Radio Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (isMuted) "إلغاء الكتم" else "كتم الصوت"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                FilledTonalIconButton(
                    onClick = onPreviousChannel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_prev_channel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "القناة السابقة"
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = OnAirCrimson,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("radio_play_pause_button")
                ) {
                    AnimatedContent(targetState = isPlaying, label = "radio_play_icon") { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledTonalIconButton(
                    onClick = onNextChannel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_next_channel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "القناة التالية"
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveArabicChannelCard(
    channel: LiveArabicChannel,
    isSelected: Boolean,
    mediaMode: LiveMediaMode,
    onSelect: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    val accentColor = channelAccentColor(channel.accentIndex)
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .testTag("live_tv_channel_${channel.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Channel Emblem Badge
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = if (isSelected) 0.25f else 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.shortBadgeAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Channel Metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.nameAr,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = channel.countryAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = channel.taglineAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status / Action Trailing
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = OnAirCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (mediaMode == LiveMediaMode.RADIO_AUDIO) {
                                Icons.Filled.Headphones
                            } else {
                                Icons.Filled.LiveTv
                            },
                            contentDescription = null,
                            tint = OnAirCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "يعمل",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnAirCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "تشغيل ${channel.nameAr}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (onDeleteCustom != null) {
                IconButton(
                    onClick = onDeleteCustom,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_custom_yt_${channel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف القناة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomYouTubeChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (nameAr: String, linkOrHandle: String) -> Unit
) {
    var channelName by rememberSaveable { mutableStateOf("") }
    var youtubeLink by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_yt_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text(stringResource(R.string.label_channel_name)) },
                    placeholder = { Text(stringResource(R.string.hint_yt_channel_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_yt_name")
                )
                OutlinedTextField(
                    value = youtubeLink,
                    onValueChange = { youtubeLink = it },
                    label = { Text(stringResource(R.string.hint_yt_channel_link)) },
                    placeholder = { Text("https://youtube.com/@channel/live") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_yt_link")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(channelName, youtubeLink) },
                enabled = channelName.isNotBlank() && youtubeLink.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnAirCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_add_yt_channel")
            ) {
                Text("إضافة وتشغيل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun channelAccentColor(index: Int): Color = when (index % 5) {
    0 -> OnAirCrimson
    1 -> BroadcastGold
    2 -> VoiceSignalCyan
    3 -> StudioEmerald
    else -> Color(0xFFA78BFA)
}
