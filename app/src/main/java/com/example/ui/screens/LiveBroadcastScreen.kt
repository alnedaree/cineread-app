package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RadioCatalog
import com.example.data.model.RadioChannel
import com.example.data.model.VoiceEngineSource
import com.example.ui.components.AnchorSpotlightAvatar
import com.example.ui.components.LiveAudioSpectrumVisualizer
import com.example.ui.components.LiveBreakingTicker
import com.example.ui.components.OnAirTallyBadge
import com.example.ui.components.channelAccentColor
import com.example.ui.theme.BroadcastGold
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TelemetryFontFamily
import com.example.ui.theme.VoiceSignalCyan
import com.example.viewmodel.BroadcastStage
import com.example.viewmodel.RadioUiState
import java.util.Locale

@Composable
fun LiveBroadcastScreen(
    uiState: RadioUiState,
    spectrumLevels: List<Float>,
    onTogglePlayPause: () -> Unit,
    onTriggerBreakingFlash: () -> Unit,
    onNextTurn: () -> Unit,
    onPreviousTurn: () -> Unit,
    onSelectChannel: (RadioChannel) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleAutoBroadcast: (Boolean) -> Unit,
    onOpenScheduleTab: () -> Unit,
    onOpenCategorySelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channel = uiState.selectedChannel
    val accent = channelAccentColor(channel.accentIndex)
    val channelAnchors = channel.anchorIds.map { RadioCatalog.anchorById(it) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Studio Masthead
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = VoiceSignalCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (uiState.activeVoiceSource == VoiceEngineSource.GOOGLE_AI_VOICE) {
                                    "مدعوم بـ Google AI Voice • Gemini"
                                } else {
                                    "الصوت المحلي الاحتياطي نشط"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = VoiceSignalCyan
                            )
                        }
                    }
                }

                OnAirTallyBadge(
                    isLive = uiState.stage == BroadcastStage.ON_AIR,
                    isLoading = uiState.isBusyGenerating
                )
            }

            // Live Breaking News Ticker
            LiveBreakingTicker(
                headlines = uiState.currentEpisode?.breakingTickerHeadlines.orEmpty(),
                modifier = Modifier.testTag("breaking_news_ticker")
            )

            // Active Personalized News Categories Strip + Customize Button
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OnAirCrimson.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenCategorySelection)
                            .testTag("customize_categories_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.action_customize_categories),
                                tint = OnAirCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_customize_categories),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnAirCrimson
                            )
                        }
                    }
                }
                items(uiState.selectedCategories, key = { it.id }) { cat ->
                    val catAccent = channelAccentColor(cat.accentIndex)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, catAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenCategorySelection)
                    ) {
                        Text(
                            text = cat.nameAr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Signature Studio Broadcast Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (uiState.stage == BroadcastStage.ON_AIR) accent.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                ),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Studio Art Banner with Frequency Dial & Program Info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero),
                            contentDescription = "أستوديو أثير الأخبار",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            StudioObsidian.copy(alpha = 0.40f),
                                            StudioObsidian.copy(alpha = 0.72f),
                                            StudioObsidian.copy(alpha = 0.96f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accent.copy(alpha = 0.22f),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = channel.categoryAr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = channel.nameAr,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = uiState.currentEpisode?.programTitleAr ?: channel.taglineAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.82f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = channel.frequencyLabel,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = TelemetryFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = BroadcastGold
                                    )
                                    Text(
                                        text = "AI LIVE FM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = TelemetryFontFamily,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            // Real-time audio waveform visualizer overlaid cleanly at the studio console base
                            LiveAudioSpectrumVisualizer(
                                levels = spectrumLevels,
                                isActive = uiState.stage == BroadcastStage.ON_AIR,
                                accentColor = accent
                            )
                        }
                    }

                    // Anchor Desk & Transport Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Active Studio Anchors Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            channelAnchors.forEach { anchor ->
                                val isSpeakingNow = uiState.stage == BroadcastStage.ON_AIR &&
                                    uiState.currentTurn?.anchorId == anchor.id
                                AnchorSpotlightAvatar(
                                    anchor = anchor,
                                    isSpeaking = isSpeakingNow,
                                    accentColor = accent,
                                    size = if (isSpeakingNow) 64.dp else 54.dp
                                )
                            }
                        }

                        // Pipeline Progress / Status Banner
                        AnimatedVisibility(
                            visible = uiState.isBusyGenerating,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = accent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.2.dp,
                                            color = accent
                                        )
                                        Text(
                                            text = uiState.statusDetailAr.ifBlank {
                                                stringResource(R.string.status_fetching_news)
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = accent,
                                        trackColor = accent.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }

                        // Error Banner (transparent error propagation!)
                        AnimatedVisibility(
                            visible = uiState.stage == BroadcastStage.ERROR && uiState.errorMessageAr != null
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = uiState.errorMessageAr.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.archivedEpisodes.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = onOpenScheduleTab,
                                                modifier = Modifier.testTag("error_open_archive_button")
                                            ) {
                                                Text("الأرشيف")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = onTogglePlayPause,
                                            modifier = Modifier.testTag("retry_broadcast_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.action_retry))
                                        }
                                    }
                                }
                            }
                        }

                        // Optional Voice Fallback Transparency Notice
                        uiState.voiceFallbackNoticeAr?.let { notice ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BroadcastGold.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = notice,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BroadcastGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }

                        // Main Broadcast Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onCycleSpeed)
                                    .testTag("playback_speed_button")
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2gx", uiState.settings.playbackSpeed),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = TelemetryFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }

                            // Previous Turn
                            FilledTonalIconButton(
                                onClick = onPreviousTurn,
                                enabled = uiState.currentEpisode != null && uiState.currentTurnIndex > 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("prev_turn_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = stringResource(R.string.action_prev_turn)
                                )
                            }

                            // Primary ON-AIR Play/Pause Button
                            Button(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(66.dp)
                                    .testTag("main_play_pause_button")
                            ) {
                                if (uiState.isBusyGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (uiState.stage == BroadcastStage.ON_AIR) {
                                            Icons.Filled.Pause
                                        } else {
                                            Icons.Filled.PlayArrow
                                        },
                                        contentDescription = if (uiState.stage == BroadcastStage.ON_AIR) {
                                            stringResource(R.string.action_pause_broadcast)
                                        } else {
                                            stringResource(R.string.action_start_broadcast)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            // Next Turn
                            FilledTonalIconButton(
                                onClick = onNextTurn,
                                enabled = uiState.currentEpisode != null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("next_turn_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = stringResource(R.string.action_next_turn)
                                )
                            }

                            // Breaking News Flash Button
                            FilledTonalIconButton(
                                onClick = onTriggerBreakingFlash,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = OnAirCrimson.copy(alpha = 0.16f),
                                    contentColor = OnAirCrimson
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("breaking_flash_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = stringResource(R.string.action_breaking_flash)
                                )
                            }
                        }

                        // Quick CTA row if standby with no episode yet
                        if (uiState.currentEpisode == null && !uiState.isBusyGenerating && uiState.stage != BroadcastStage.ERROR) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onTogglePlayPause,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("start_live_cta_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_start_broadcast),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                OutlinedButton(
                                    onClick = onTriggerBreakingFlash,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("urgent_flash_cta_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = OnAirCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_breaking_flash),
                                        color = OnAirCrimson,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Synchronized Teleprompter Card
            val currentTurn = uiState.currentTurn
            if (currentTurn != null) {
                val activeAnchor = RadioCatalog.anchorById(currentTurn.anchorId)
                val episode = uiState.currentEpisode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_teleprompter_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = currentTurn.segmentType.labelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${uiState.currentTurnIndex + 1} / ${episode?.turns?.size ?: 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = TelemetryFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "عرض النشرة الكاملة",
                                style = MaterialTheme.typography.labelMedium,
                                color = VoiceSignalCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenScheduleTab)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("open_full_rundown_button")
                            )
                        }

                        Text(
                            text = currentTurn.headlineTag,
                            style = MaterialTheme.typography.titleMedium,
                            color = BroadcastGold
                        )

                        Text(
                            text = currentTurn.textAr,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeAnchor.nameAr} • ${currentTurn.emotionCue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.nextTurn?.let { upcoming ->
                                val nextAnchor = RadioCatalog.anchorById(upcoming.anchorId)
                                Text(
                                    text = "التالي: ${nextAnchor.nameAr.substringBefore(" ")} — ${upcoming.headlineTag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 210.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Continuous 24/7 Auto-Broadcast Switch Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_auto_broadcast),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يتابع الأخبار تلقائياً ويعدّ النشرة التالية فور انتهاء البرنامج",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.settings.autoContinuousBroadcast,
                        onCheckedChange = onToggleAutoBroadcast,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        ),
                        modifier = Modifier.testTag("auto_broadcast_switch")
                    )
                }
            }

            // Quick Frequency Channel Dial Strip
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.label_channel_dial),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(uiState.channels, key = { it.id }) { item ->
                        val isSelected = item.id == channel.id
                        val itemAccent = channelAccentColor(item.accentIndex)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) itemAccent.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) itemAccent
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectChannel(item) }
                                .testTag("quick_channel_${item.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.frequencyLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = TelemetryFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = itemAccent
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(itemAccent)
                                        )
                                    }
                                }
                                Text(
                                    text = item.nameAr,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.categoryAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
