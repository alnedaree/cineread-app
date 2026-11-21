package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BroadcastStyle
import com.example.data.model.LiveArabicChannel
import com.example.data.model.LiveArabicChannelsCatalog
import com.example.data.model.LiveMediaMode
import com.example.data.model.RadioCatalog
import com.example.data.model.RadioChannel
import com.example.data.model.RadioJsonSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.radioDataStore: DataStore<Preferences> by preferencesDataStore(name = "atheer_radio_prefs")

data class RadioUserSettings(
    val selectedChannelId: String = "breaking_general",
    val broadcastStyle: BroadcastStyle = BroadcastStyle.PODCAST_DIALOGUE,
    val autoContinuousBroadcast: Boolean = true,
    val studioChimeEnabled: Boolean = true,
    val allowDeviceTtsFallback: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val darkStudioTheme: Boolean = true,
    val customChannels: List<RadioChannel> = emptyList(),
    val selectedCategoryIds: Set<String> = RadioCatalog.defaultSelectedCategoryIds,
    val hasConfiguredCategories: Boolean = false,
    val liveMediaMode: LiveMediaMode = LiveMediaMode.VIDEO,
    val customLiveArabicChannels: List<LiveArabicChannel> = emptyList()
)

class RadioPreferencesRepository(private val context: Context) {

    private object Keys {
        val SELECTED_CHANNEL_ID = stringPreferencesKey("selected_channel_id")
        val BROADCAST_STYLE = stringPreferencesKey("broadcast_style")
        val AUTO_CONTINUOUS = booleanPreferencesKey("auto_continuous")
        val STUDIO_CHIME = booleanPreferencesKey("studio_chime")
        val ALLOW_DEVICE_FALLBACK = booleanPreferencesKey("allow_device_fallback")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val DARK_STUDIO_THEME = booleanPreferencesKey("dark_studio_theme")
        val CUSTOM_CHANNELS_JSON = stringPreferencesKey("custom_channels_json")
        val SELECTED_CATEGORY_IDS = stringSetPreferencesKey("selected_category_ids")
        val HAS_CONFIGURED_CATEGORIES = booleanPreferencesKey("has_configured_categories")
        val LIVE_MEDIA_MODE = stringPreferencesKey("live_media_mode")
        val CUSTOM_LIVE_ARABIC_JSON = stringPreferencesKey("custom_live_arabic_json")
    }

    val settingsFlow: Flow<RadioUserSettings> = context.radioDataStore.data.map { prefs ->
        val savedCategories = prefs[Keys.SELECTED_CATEGORY_IDS]
        RadioUserSettings(
            selectedChannelId = prefs[Keys.SELECTED_CHANNEL_ID] ?: "breaking_general",
            broadcastStyle = BroadcastStyle.fromId(prefs[Keys.BROADCAST_STYLE]),
            autoContinuousBroadcast = prefs[Keys.AUTO_CONTINUOUS] ?: true,
            studioChimeEnabled = prefs[Keys.STUDIO_CHIME] ?: true,
            allowDeviceTtsFallback = prefs[Keys.ALLOW_DEVICE_FALLBACK] ?: true,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            darkStudioTheme = prefs[Keys.DARK_STUDIO_THEME] ?: true,
            customChannels = RadioJsonSerializer.deserializeCustomChannels(
                prefs[Keys.CUSTOM_CHANNELS_JSON].orEmpty()
            ),
            selectedCategoryIds = if (savedCategories.isNullOrEmpty()) {
                RadioCatalog.defaultSelectedCategoryIds
            } else {
                savedCategories
            },
            hasConfiguredCategories = prefs[Keys.HAS_CONFIGURED_CATEGORIES] ?: false,
            liveMediaMode = LiveMediaMode.fromId(prefs[Keys.LIVE_MEDIA_MODE]),
            customLiveArabicChannels = LiveArabicChannelsCatalog.deserializeCustomList(
                prefs[Keys.CUSTOM_LIVE_ARABIC_JSON].orEmpty()
            )
        )
    }

    suspend fun setLiveMediaMode(mode: LiveMediaMode) {
        context.radioDataStore.edit { it[Keys.LIVE_MEDIA_MODE] = mode.id }
    }

    suspend fun addCustomLiveArabicChannel(channel: LiveArabicChannel) {
        context.radioDataStore.edit { prefs ->
            val current = LiveArabicChannelsCatalog.deserializeCustomList(
                prefs[Keys.CUSTOM_LIVE_ARABIC_JSON].orEmpty()
            )
            prefs[Keys.CUSTOM_LIVE_ARABIC_JSON] =
                LiveArabicChannelsCatalog.serializeCustomList(current + channel)
        }
    }

    suspend fun removeCustomLiveArabicChannel(channelId: String) {
        context.radioDataStore.edit { prefs ->
            val current = LiveArabicChannelsCatalog.deserializeCustomList(
                prefs[Keys.CUSTOM_LIVE_ARABIC_JSON].orEmpty()
            )
            prefs[Keys.CUSTOM_LIVE_ARABIC_JSON] =
                LiveArabicChannelsCatalog.serializeCustomList(current.filterNot { it.id == channelId })
        }
    }

    suspend fun setSelectedCategories(categoryIds: Set<String>) {
        if (categoryIds.isEmpty()) return
        context.radioDataStore.edit { prefs ->
            prefs[Keys.SELECTED_CATEGORY_IDS] = categoryIds
            prefs[Keys.HAS_CONFIGURED_CATEGORIES] = true
        }
    }

    suspend fun markCategoriesConfigured() {
        context.radioDataStore.edit { prefs ->
            prefs[Keys.HAS_CONFIGURED_CATEGORIES] = true
        }
    }

    suspend fun setSelectedChannel(channelId: String) {
        context.radioDataStore.edit { it[Keys.SELECTED_CHANNEL_ID] = channelId }
    }

    suspend fun setBroadcastStyle(style: BroadcastStyle) {
        context.radioDataStore.edit { it[Keys.BROADCAST_STYLE] = style.id }
    }

    suspend fun setAutoContinuous(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.AUTO_CONTINUOUS] = enabled }
    }

    suspend fun setStudioChime(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.STUDIO_CHIME] = enabled }
    }

    suspend fun setAllowDeviceFallback(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.ALLOW_DEVICE_FALLBACK] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.radioDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setDarkStudioTheme(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.DARK_STUDIO_THEME] = enabled }
    }

    suspend fun addCustomChannel(channel: RadioChannel) {
        context.radioDataStore.edit { prefs ->
            val current = RadioJsonSerializer.deserializeCustomChannels(
                prefs[Keys.CUSTOM_CHANNELS_JSON].orEmpty()
            )
            val updated = current + channel
            prefs[Keys.CUSTOM_CHANNELS_JSON] = RadioJsonSerializer.serializeCustomChannels(updated)
            prefs[Keys.SELECTED_CHANNEL_ID] = channel.id
        }
    }

    suspend fun removeCustomChannel(channelId: String) {
        context.radioDataStore.edit { prefs ->
            val current = RadioJsonSerializer.deserializeCustomChannels(
                prefs[Keys.CUSTOM_CHANNELS_JSON].orEmpty()
            )
            val updated = current.filterNot { it.id == channelId }
            prefs[Keys.CUSTOM_CHANNELS_JSON] = RadioJsonSerializer.serializeCustomChannels(updated)
            if (prefs[Keys.SELECTED_CHANNEL_ID] == channelId) {
                prefs[Keys.SELECTED_CHANNEL_ID] = "breaking_general"
            }
        }
    }
}
