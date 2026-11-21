package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RadioAnchor
import com.example.ui.theme.BroadcastGold
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.TelemetryFontFamily
import com.example.ui.theme.VoiceSignalCyan
import com.example.viewmodel.BroadcastStage
import com.example.viewmodel.RadioUiState

fun channelAccentColor(accentIndex: Int): Color = when (accentIndex % 5) {
    0 -> OnAirCrimson
    1 -> BroadcastGold
    2 -> VoiceSignalCyan
    3 -> StudioEmerald
    else -> Color(0xFFA78BFA)
}

@Composable
fun OnAirTallyBadge(
    isLive: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tally_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tally_alpha"
    )

    val badgeColor = when {
        isLive -> OnAirCrimson
        isLoading -> BroadcastGold
        else -> MaterialTheme.colorScheme.outline
    }

    val labelText = when {
        isLive -> stringResource(R.string.badge_on_air)
        isLoading -> "جاري التجهيز…"
        else -> stringResource(R.string.badge_standby)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = if (isLive || isLoading) 0.18f else 0.25f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLive || isLoading) badgeColor.copy(alpha = pulseAlpha)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = if (isLive || isLoading) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LiveBreakingTicker(
    headlines: List<String>,
    modifier: Modifier = Modifier
) {
    val displayText = if (headlines.isEmpty()) {
        "شبكة أثير الأخبار الذكية • رصد تلقائي للأخبار اليومية والعاجلة وبث مباشر بأصوات Google AI Voice"
    } else {
        headlines.joinToString("   ◆   ")
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OnAirCrimson
            ) {
                Text(
                    text = stringResource(R.string.label_breaking_news),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE, velocity = 36.dp)
            )
        }
    }
}

@Composable
fun LiveAudioSpectrumVisualizer(
    levels: List<Float>,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val secondaryColor = BroadcastGold
    val idleColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val barCount = levels.size.coerceAtLeast(1)
        val gap = 6.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(3.dp.toPx())
        val centerY = size.height / 2f

        val gradientBrush = Brush.verticalGradient(
            colors = if (isActive) listOf(accentColor, secondaryColor, accentColor)
            else listOf(idleColor, idleColor)
        )

        levels.forEachIndexed { index, rawLevel ->
            val level = if (isActive) rawLevel.coerceIn(0.12f, 1f) else 0.14f
            val barHeight = (size.height * level).coerceAtLeast(6.dp.toPx())
            val x = index * (barWidth + gap)
            val y = centerY - barHeight / 2f

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun AnchorSpotlightAvatar(
    anchor: RadioAnchor,
    isSpeaking: Boolean,
    accentColor: Color,
    size: Dp = 68.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_ring")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size + 10.dp)
                .clip(CircleShape)
                .background(
                    if (isSpeaking) accentColor.copy(alpha = 0.22f * ringScale)
                    else Color.Transparent
                )
                .border(
                    width = if (isSpeaking) 2.5.dp else 1.dp,
                    color = if (isSpeaking) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = anchor.avatarRes),
                contentDescription = anchor.nameAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = anchor.nameAr.substringBefore(" "),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSpeaking) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun MiniBroadcastPlayerBar(
    uiState: RadioUiState,
    onTogglePlayPause: () -> Unit,
    onNextTurn: () -> Unit,
    onOpenLiveTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = channelAccentColor(uiState.selectedChannel.accentIndex)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpenLiveTab)
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = uiState.currentAnchor.avatarRes),
                contentDescription = uiState.currentAnchor.nameAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = uiState.selectedChannel.nameAr,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.selectedChannel.frequencyLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TelemetryFontFamily
                        ),
                        color = accent
                    )
                }
                Text(
                    text = uiState.currentTurn?.headlineTag
                        ?: uiState.statusDetailAr.ifBlank { uiState.selectedChannel.taglineAr },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (uiState.isBusyGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = accent
                )
            } else {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.stage == BroadcastStage.ON_AIR) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (uiState.stage == BroadcastStage.ON_AIR) {
                            stringResource(R.string.action_pause_broadcast)
                        } else {
                            stringResource(R.string.action_resume_broadcast)
                        },
                        tint = accent
                    )
                }
            }
            IconButton(
                onClick = onNextTurn,
                enabled = uiState.currentEpisode != null,
                modifier = Modifier.testTag("mini_player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.action_next_turn),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
