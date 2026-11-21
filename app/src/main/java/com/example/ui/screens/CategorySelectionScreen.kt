package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.NewsCategory
import com.example.data.model.RadioCatalog
import com.example.ui.components.channelAccentColor
import com.example.ui.theme.BroadcastGold
import com.example.ui.theme.OnAirCrimson
import com.example.ui.theme.VoiceSignalCyan

@Composable
fun CategorySelectionScreen(
    initialSelectedIds: Set<String>,
    isOnboarding: Boolean,
    onApplyAndBroadcast: (Set<String>) -> Unit,
    onSaveOnly: (Set<String>) -> Unit,
    onBackOrSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIds by remember(initialSelectedIds) {
        mutableStateOf(
            if (initialSelectedIds.isEmpty()) RadioCatalog.defaultSelectedCategoryIds
            else initialSelectedIds
        )
    }

    if (!isOnboarding) {
        BackHandler(onBack = onBackOrSkip)
    }

    val allCategories = RadioCatalog.newsCategories
    val canConfirm = selectedIds.isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!canConfirm) {
                            Text(
                                text = stringResource(R.string.hint_select_at_least_one),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onApplyAndBroadcast(selectedIds) },
                                enabled = canConfirm,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OnAirCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("apply_categories_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_apply_categories),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = { onSaveOnly(selectedIds) },
                                enabled = canConfirm,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("save_categories_only_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.action_save_categories_only),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                val columns = if (maxWidth >= 560.dp) 3 else 2
                val rows = allCategories.chunked(columns)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!isOnboarding) {
                                    IconButton(
                                        onClick = onBackOrSkip,
                                        modifier = Modifier.testTag("categories_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "رجوع",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = OnAirCrimson.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.45f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Tune,
                                            contentDescription = null,
                                            tint = OnAirCrimson,
                                            modifier = Modifier
                                                .padding(9.dp)
                                                .size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.title_category_selection),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            if (isOnboarding) {
                                TextButton(
                                    onClick = onBackOrSkip,
                                    modifier = Modifier.testTag("categories_skip_button")
                                ) {
                                    Text(
                                        text = "تخطي",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Subtitle & Studio Editorial Intro Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                OnAirCrimson.copy(alpha = 0.14f),
                                                BroadcastGold.copy(alpha = 0.08f),
                                                VoiceSignalCyan.copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = stringResource(R.string.subtitle_category_selection),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = OnAirCrimson.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, OnAirCrimson.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "تم اختيار ${selectedIds.size} من ${allCategories.size}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = OnAirCrimson,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(
                                                onClick = {
                                                    selectedIds = allCategories.map { it.id }.toSet()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("categories_select_all_button")
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.action_select_all),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = VoiceSignalCyan
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    selectedIds = RadioCatalog.defaultSelectedCategoryIds
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("categories_reset_button")
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.action_reset_defaults),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = BroadcastGold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Responsive Multi-Select Category Cards Grid
                    items(rows.size) { rowIndex ->
                        val rowCategories = rows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCategories.forEach { category ->
                                val isSelected = category.id in selectedIds
                                CategoryOptionCard(
                                    category = category,
                                    isSelected = isSelected,
                                    onToggle = {
                                        selectedIds = if (isSelected) {
                                            selectedIds - category.id
                                        } else {
                                            selectedIds + category.id
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowCategories.size < columns) {
                                repeat(columns - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionCard(
    category: NewsCategory,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = channelAccentColor(category.accentIndex)
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cat_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cat_border"
    )

    Surface(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor),
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = if (isSelected) 0.28f else 0.14f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(category.iconKey),
                            contentDescription = category.nameAr,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) accent
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "مختار",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = category.nameAr,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.subtitleAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "yemen" -> Icons.Filled.LocationOn
    "politics" -> Icons.Filled.AccountBalance
    "tech" -> Icons.Filled.Memory
    "sports" -> Icons.Filled.SportsSoccer
    "economy" -> Icons.Filled.TrendingUp
    "arab" -> Icons.Filled.Public
    "science" -> Icons.Filled.Science
    else -> Icons.Filled.AutoStories
}
