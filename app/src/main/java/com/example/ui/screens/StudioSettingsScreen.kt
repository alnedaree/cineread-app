package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BroadcastStyle
import com.example.data.model.RadioAnchor
import com.example.data.model.RadioCatalog
import com.example.ui.theme.BroadcastGold
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.VoiceSignalCyan
import com.example.viewmodel.RadioUiState

@Composable
fun StudioSettingsScreen(
    uiState: RadioUiState,
    onTestAnchorVoice: (RadioAnchor) -> Unit,
    onSelectBroadcastStyle: (BroadcastStyle) -> Unit,
    onToggleAutoContinuous: (Boolean) -> Unit,
    onToggleStudioChime: (Boolean) -> Unit,
    onToggleDeviceFallback: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.title_studio),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.subtitle_studio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Google AI Voice Engine Status Banner
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VoiceSignalCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, VoiceSignalCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VoiceSignalCyan.copy(alpha = 0.22f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = VoiceSignalCyan
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "متصل بمحرك Google AI Voice (Gemini TTS)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توليد صوت إذاعي حي ومتعدد الأصوات لكل مذيع مع رصد الأخبار عبر Google Search Grounding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Voice Test Error (transparent error surfacing)
            uiState.voiceTestErrorAr?.let { err ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.section_anchors),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(RadioCatalog.anchors, key = { it.id }) { anchor ->
                val isTesting = uiState.testingAnchorId == anchor.id
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = anchor.avatarRes),
                            contentDescription = anchor.nameAr,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(2.dp, BroadcastGold.copy(alpha = 0.7f), CircleShape)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = anchor.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = anchor.roleAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = BroadcastGold
                            )
                            Text(
                                text = anchor.voiceDescriptorAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = VoiceSignalCyan
                            )
                            Text(
                                text = anchor.bioAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onTestAnchorVoice(anchor) },
                                enabled = uiState.testingAnchorId == null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("test_voice_${anchor.id}")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.label_testing_voice))
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_test_voice))
                                }
                            }
                        }
                    }
                }
            }

            // Broadcast Style Selector
            item {
                Text(
                    text = stringResource(R.string.section_broadcast_style),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val styleOptions = listOf(
                    Triple(
                        BroadcastStyle.PODCAST_DIALOGUE,
                        stringResource(R.string.style_podcast_title),
                        stringResource(R.string.style_podcast_desc)
                    ),
                    Triple(
                        BroadcastStyle.NEWSROOM_FORMAL,
                        stringResource(R.string.style_formal_title),
                        stringResource(R.string.style_formal_desc)
                    ),
                    Triple(
                        BroadcastStyle.RAPID_FLASH,
                        stringResource(R.string.style_rapid_title),
                        stringResource(R.string.style_rapid_desc)
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    styleOptions.forEach { (style, title, desc) ->
                        val selected = uiState.settings.broadcastStyle == style
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) OnAirCrimson.copy(alpha = 0.13f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) OnAirCrimson
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectBroadcastStyle(style) }
                                .testTag("style_option_${style.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectBroadcastStyle(style) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = OnAirCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Automation & Audio Preferences
            item {
                Text(
                    text = stringResource(R.string.section_automation),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StudioToggleRow(
                            title = stringResource(R.string.setting_auto_dj_title),
                            description = stringResource(R.string.setting_auto_dj_desc),
                            checked = uiState.settings.autoContinuousBroadcast,
                            onCheckedChange = onToggleAutoContinuous,
                            testTag = "settings_auto_dj_switch"
                        )
                        StudioToggleRow(
                            title = stringResource(R.string.setting_chime_title),
                            description = stringResource(R.string.setting_chime_desc),
                            checked = uiState.settings.studioChimeEnabled,
                            onCheckedChange = onToggleStudioChime,
                            testTag = "settings_chime_switch"
                        )
                        StudioToggleRow(
                            title = stringResource(R.string.setting_fallback_title),
                            description = stringResource(R.string.setting_fallback_desc),
                            checked = uiState.settings.allowDeviceTtsFallback,
                            onCheckedChange = onToggleDeviceFallback,
                            testTag = "settings_fallback_switch"
                        )
                        StudioToggleRow(
                            title = stringResource(R.string.setting_dark_theme_title),
                            description = stringResource(R.string.setting_dark_theme_desc),
                            checked = uiState.settings.darkStudioTheme,
                            onCheckedChange = onToggleDarkTheme,
                            testTag = "settings_dark_theme_switch"
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OnAirCrimson
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
