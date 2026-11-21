package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.MiniBroadcastPlayerBar
import com.example.ui.screens.CategorySelectionScreen
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.LiveArabicTvScreen
import com.example.ui.screens.LiveBroadcastScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.StudioSettingsScreen
import com.example.ui.theme.AtheerRadioTheme
import com.example.viewmodel.BroadcastStage
import com.example.viewmodel.RadioViewModel

private enum class RadioTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    LIVE(R.string.nav_live, Icons.Filled.Radio, Icons.Outlined.Radio, "nav_tab_live"),
    LIVE_TV(R.string.nav_live_tv, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_tab_live_tv"),
    CHANNELS(R.string.nav_channels, Icons.Filled.Podcasts, Icons.Outlined.Podcasts, "nav_tab_channels"),
    SCHEDULE(R.string.nav_schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_schedule"),
    STUDIO(R.string.nav_studio, Icons.Filled.Mic, Icons.Outlined.MicNone, "nav_tab_studio")
}

@Composable
fun AtheerRadioApp(
    viewModel: RadioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spectrumLevels by viewModel.spectrumLevels.collectAsStateWithLifecycle()
    var activeTab by rememberSaveable { mutableStateOf(RadioTab.LIVE) }
    var showCategoryEditor by rememberSaveable { mutableStateOf(false) }

    val isCategoryOnboarding = uiState.settingsLoaded && !uiState.settings.hasConfiguredCategories
    val showingCategoriesScreen = isCategoryOnboarding || showCategoryEditor

    // Ensure hardware/gesture Back returns cleanly to the Live Studio
    BackHandler(enabled = !showingCategoriesScreen && activeTab != RadioTab.LIVE) {
        activeTab = RadioTab.LIVE
    }

    val showMiniPlayer = !showingCategoriesScreen &&
        activeTab != RadioTab.LIVE &&
        activeTab != RadioTab.LIVE_TV &&
        (uiState.currentEpisode != null || uiState.isBusyGenerating || uiState.stage == BroadcastStage.ON_AIR)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AtheerRadioTheme(darkTheme = uiState.settings.darkStudioTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (showingCategoriesScreen) {
                    CategorySelectionScreen(
                        initialSelectedIds = uiState.settings.selectedCategoryIds,
                        isOnboarding = isCategoryOnboarding && !showCategoryEditor,
                        onApplyAndBroadcast = { chosenIds ->
                            viewModel.saveSelectedCategories(chosenIds, startBroadcastNow = true)
                            showCategoryEditor = false
                            activeTab = RadioTab.LIVE
                        },
                        onSaveOnly = { chosenIds ->
                            viewModel.saveSelectedCategories(chosenIds, startBroadcastNow = false)
                            showCategoryEditor = false
                        },
                        onBackOrSkip = {
                            if (isCategoryOnboarding && !showCategoryEditor) {
                                viewModel.dismissCategoryOnboarding()
                            }
                            showCategoryEditor = false
                        }
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavRail = maxWidth >= 600.dp

                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                if (!useNavRail) {
                                    Column {
                                        if (showMiniPlayer) {
                                            MiniBroadcastPlayerBar(
                                                uiState = uiState,
                                                onTogglePlayPause = viewModel::togglePlayPause,
                                                onNextTurn = viewModel::nextTurn,
                                                onOpenLiveTab = { activeTab = RadioTab.LIVE }
                                            )
                                        }
                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 4.dp
                                        ) {
                                            RadioTab.entries.forEach { tab ->
                                                val selected = activeTab == tab
                                                NavigationBarItem(
                                                    selected = selected,
                                                    onClick = { activeTab = tab },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                            contentDescription = stringResource(tab.labelRes)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = stringResource(tab.labelRes),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    modifier = Modifier.testTag(tab.tag)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useNavRail) {
                                    NavigationRail(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        RadioTab.entries.forEach { tab ->
                                            val selected = activeTab == tab
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { activeTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = stringResource(tab.labelRes)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = stringResource(tab.labelRes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                modifier = Modifier.testTag(tab.tag)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (activeTab) {
                                            RadioTab.LIVE -> LiveBroadcastScreen(
                                                uiState = uiState,
                                                spectrumLevels = spectrumLevels,
                                                onTogglePlayPause = viewModel::togglePlayPause,
                                                onTriggerBreakingFlash = viewModel::triggerBreakingFlash,
                                                onNextTurn = viewModel::nextTurn,
                                                onPreviousTurn = viewModel::previousTurn,
                                                onSelectChannel = { ch ->
                                                    viewModel.selectChannel(ch, autoStartIfLive = true)
                                                },
                                                onCycleSpeed = viewModel::cyclePlaybackSpeed,
                                                onToggleAutoBroadcast = viewModel::toggleAutoContinuous,
                                                onOpenScheduleTab = { activeTab = RadioTab.SCHEDULE },
                                                onOpenCategorySelection = { showCategoryEditor = true }
                                            )

                                            RadioTab.LIVE_TV -> LiveArabicTvScreen(
                                                uiState = uiState,
                                                onScreenEntered = viewModel::ensureLiveArabicChannelResolved,
                                                onSelectChannel = viewModel::selectLiveArabicChannel,
                                                onNextChannel = viewModel::selectNextLiveArabicChannel,
                                                onPreviousChannel = viewModel::selectPreviousLiveArabicChannel,
                                                onReloadStream = viewModel::reloadLiveArabicStream,
                                                onSetMediaMode = viewModel::setLiveMediaMode,
                                                onAddCustomChannel = viewModel::addCustomLiveArabicChannel,
                                                onRemoveCustomChannel = viewModel::removeCustomLiveArabicChannel
                                            )

                                            RadioTab.CHANNELS -> ChannelsScreen(
                                                uiState = uiState,
                                                onTuneChannel = { ch ->
                                                    viewModel.tuneAndBroadcastChannel(ch)
                                                    activeTab = RadioTab.LIVE
                                                },
                                                onCreateChannel = { name, topic, anchors ->
                                                    viewModel.createCustomChannel(name, topic, anchors)
                                                    activeTab = RadioTab.LIVE
                                                },
                                                onDeleteCustomChannel = viewModel::deleteCustomChannel
                                            )

                                            RadioTab.SCHEDULE -> ScheduleScreen(
                                                uiState = uiState,
                                                onJumpToTurn = { turnIdx ->
                                                    viewModel.jumpToTurn(turnIdx)
                                                    activeTab = RadioTab.LIVE
                                                },
                                                onReplayEpisode = { ep ->
                                                    viewModel.playArchivedEpisode(ep)
                                                    activeTab = RadioTab.LIVE
                                                },
                                                onDeleteEpisode = viewModel::deleteArchivedEpisode,
                                                onStartFreshBroadcast = {
                                                    viewModel.startLiveBroadcast(isBreakingFlash = false)
                                                    activeTab = RadioTab.LIVE
                                                }
                                            )

                                            RadioTab.STUDIO -> StudioSettingsScreen(
                                                uiState = uiState,
                                                onTestAnchorVoice = viewModel::testAnchorVoice,
                                                onSelectBroadcastStyle = viewModel::updateBroadcastStyle,
                                                onToggleAutoContinuous = viewModel::toggleAutoContinuous,
                                                onToggleStudioChime = viewModel::toggleStudioChime,
                                                onToggleDeviceFallback = viewModel::toggleDeviceTtsFallback,
                                                onToggleDarkTheme = viewModel::toggleDarkStudioTheme
                                            )
                                        }
                                    }

                                    if (useNavRail && showMiniPlayer) {
                                        MiniBroadcastPlayerBar(
                                            uiState = uiState,
                                            onTogglePlayPause = viewModel::togglePlayPause,
                                            onNextTurn = viewModel::nextTurn,
                                            onOpenLiveTab = { activeTab = RadioTab.LIVE }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
