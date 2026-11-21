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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RadioCatalog
import com.example.data.model.RadioChannel
import com.example.ui.components.channelAccentColor
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.TelemetryFontFamily
import com.example.viewmodel.BroadcastStage
import com.example.viewmodel.RadioUiState

@Composable
fun ChannelsScreen(
    uiState: RadioUiState,
    onTuneChannel: (RadioChannel) -> Unit,
    onCreateChannel: (nameAr: String, topicPrompt: String, anchorIds: List<String>) -> Unit,
    onDeleteCustomChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 92.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.title_channels),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.subtitle_channels),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.channels, key = { it.id }) { channel ->
                val isSelected = channel.id == uiState.selectedChannel.id
                val isLiveOnThis = isSelected && uiState.stage == BroadcastStage.ON_AIR
                val accent = channelAccentColor(channel.accentIndex)
                val anchors = channel.anchorIds.map { RadioCatalog.anchorById(it) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTuneChannel(channel) }
                        .testTag("channel_card_${channel.id}"),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) accent.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accent
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                    )
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
                                    color = accent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = channel.frequencyLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = TelemetryFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = channel.categoryAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (channel.isCustom) {
                                IconButton(
                                    onClick = { onDeleteCustomChannel(channel.id) },
                                    modifier = Modifier.testTag("delete_channel_${channel.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "حذف القناة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = channel.nameAr,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = channel.taglineAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                anchors.forEach { anchor ->
                                    Image(
                                        painter = painterResource(id = anchor.avatarRes),
                                        contentDescription = anchor.nameAr,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, accent.copy(alpha = 0.7f), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = anchors.joinToString(" • ") { it.nameAr.substringBefore(" ") },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onTuneChannel(channel) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLiveOnThis) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isLiveOnThis) Color.White else accent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("tune_channel_button_${channel.id}")
                            ) {
                                Icon(
                                    imageVector = if (isLiveOnThis) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLiveOnThis) {
                                        stringResource(R.string.label_tuned_in)
                                    } else {
                                        stringResource(R.string.action_tune_in)
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = OnAirCrimson,
            contentColor = Color.White,
            icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
            text = {
                Text(
                    text = stringResource(R.string.action_create_channel),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_channel_fab")
        )
    }

    if (showCreateDialog) {
        CreateCustomChannelDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, topic, anchorIds ->
                onCreateChannel(name, topic, anchorIds)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateCustomChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var channelTopic by remember { mutableStateOf("") }
    val selectedAnchors = remember { mutableStateListOf("tariq", "layla") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_create_channel_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text(stringResource(R.string.label_channel_name)) },
                    placeholder = { Text(stringResource(R.string.hint_channel_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_channel_name_input")
                )
                OutlinedTextField(
                    value = channelTopic,
                    onValueChange = { channelTopic = it },
                    label = { Text(stringResource(R.string.label_channel_topic)) },
                    placeholder = { Text(stringResource(R.string.hint_channel_topic)) },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_channel_topic_input")
                )
                Text(
                    text = "اختر المذيعين المشاركين في القناة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioCatalog.anchors.forEach { anchor ->
                        val isChosen = anchor.id in selectedAnchors
                        FilterChip(
                            selected = isChosen,
                            onClick = {
                                if (isChosen && selectedAnchors.size > 1) {
                                    selectedAnchors.remove(anchor.id)
                                } else if (!isChosen) {
                                    selectedAnchors.add(anchor.id)
                                }
                            },
                            label = {
                                Text(
                                    text = anchor.nameAr.substringBefore(" "),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = if (isChosen) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnAirCrimson.copy(alpha = 0.22f),
                                selectedLabelColor = OnAirCrimson
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(channelName, channelTopic, selectedAnchors.toList()) },
                enabled = channelName.isNotBlank() && channelTopic.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OnAirCrimson, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_create_channel_button")
            ) {
                Text(stringResource(R.string.action_save_channel))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_channel_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
