package com.xplayer.media.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xplayer.media.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "xplayer_user_prefs")

data class UserPrefs(
    val darkThemeMode: Int = 0, // 0 = System, 1 = Light, 2 = Dark
    val dynamicColors: Boolean = true,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val resumeVideoPlayback: Boolean = true,
    val autoSkipCorrupted: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME_MODE = intPreferencesKey("dark_theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val RESUME_VIDEO = booleanPreferencesKey("resume_video")
        val AUTO_SKIP_CORRUPTED = booleanPreferencesKey("auto_skip_corrupted")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    }

    val userPrefsFlow: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        val sortName = prefs[Keys.SORT_OPTION] ?: SortOption.DATE_DESC.name
        val sortOption = runCatching { SortOption.valueOf(sortName) }.getOrDefault(SortOption.DATE_DESC)
        UserPrefs(
            darkThemeMode = prefs[Keys.DARK_THEME_MODE] ?: 0,
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
            sortOption = sortOption,
            resumeVideoPlayback = prefs[Keys.RESUME_VIDEO] ?: true,
            autoSkipCorrupted = prefs[Keys.AUTO_SKIP_CORRUPTED] ?: true,
            defaultPlaybackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            repeatMode = prefs[Keys.REPEAT_MODE] ?: 0,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false
        )
    }

    suspend fun setDarkThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.DARK_THEME_MODE] = mode }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    suspend fun setSortOption(option: SortOption) {
        context.dataStore.edit { it[Keys.SORT_OPTION] = option.name }
    }

    suspend fun setResumeVideoPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_VIDEO] = enabled }
    }

    suspend fun setAutoSkipCorrupted(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SKIP_CORRUPTED] = enabled }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.25f, 3.0f) }
    }

    suspend fun setRepeatMode(repeatMode: Int) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = repeatMode }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }
}
