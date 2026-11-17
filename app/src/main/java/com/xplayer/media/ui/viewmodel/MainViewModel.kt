package com.xplayer.media.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xplayer.media.XPlayerApplication
import com.xplayer.media.data.local.PlaybackHistoryEntity
import com.xplayer.media.data.local.PlaylistWithMediaIds
import com.xplayer.media.data.preferences.UserPreferencesRepository
import com.xplayer.media.data.preferences.UserPrefs
import com.xplayer.media.data.repository.MediaRepository
import com.xplayer.media.domain.model.LibraryUiState
import com.xplayer.media.domain.model.LocalMediaItem
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.domain.model.SortOption
import com.xplayer.media.playback.PlaybackControllerManager
import com.xplayer.media.playback.PlaybackUiState
import com.xplayer.media.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    appContext: Context,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val playbackManager = PlaybackControllerManager(
        context = appContext,
        onProgressUpdate = { item, pos, dur ->
            viewModelScope.launch {
                mediaRepository.savePlaybackProgress(item, pos, dur)
            }
        }
    )

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.playbackState
    val errorEvents: SharedFlow<String> = playbackManager.errorEvents

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isPartialPermission = MutableStateFlow(false)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val userPrefs: StateFlow<UserPrefs> = preferencesRepository.userPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs())

    val playlists: StateFlow<List<PlaylistWithMediaIds>> = mediaRepository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackHistory: StateFlow<List<PlaybackHistoryEntity>> = mediaRepository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryState: StateFlow<LibraryUiState> = combine(
        _hasPermission,
        _isPartialPermission,
        mediaRepository.isScanning,
        mediaRepository.audioItemsFlow,
        mediaRepository.videoItemsFlow,
        _searchQuery,
        userPrefs
    ) { values ->
        val granted = values[0] as Boolean
        val partial = values[1] as Boolean
        val scanning = values[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val rawAudio = values[3] as List<LocalMediaItem>
        @Suppress("UNCHECKED_CAST")
        val rawVideo = values[4] as List<LocalMediaItem>
        val query = values[5] as String
        val prefs = values[6] as UserPrefs

        playbackManager.setAutoSkipCorrupted(prefs.autoSkipCorrupted)

        if (!granted) {
            LibraryUiState.PermissionRequired(isPartialGrant = partial)
        } else if (scanning && rawAudio.isEmpty() && rawVideo.isEmpty()) {
            LibraryUiState.Loading
        } else if (rawAudio.isEmpty() && rawVideo.isEmpty()) {
            LibraryUiState.Empty
        } else {
            val filteredAudio = FormatUtils.filterAndSort(rawAudio, query, prefs.sortOption)
            val filteredVideo = FormatUtils.filterAndSort(rawVideo, query, prefs.sortOption)
            val allCombined = (filteredAudio + filteredVideo)
            val folders = allCombined.groupBy { it.folderName }
            val favorites = allCombined.filter { it.isFavorite }

            LibraryUiState.Content(
                audioItems = filteredAudio,
                videoItems = filteredVideo,
                folders = folders,
                favorites = favorites,
                searchQuery = query,
                sortOption = prefs.sortOption
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState.Loading)

    init {
        playbackManager.initialize()
    }

    fun getMediaController() = playbackManager.mediaController

    fun updatePermissionStatus(granted: Boolean, isPartial: Boolean = false) {
        val changed = _hasPermission.value != granted
        _hasPermission.value = granted
        _isPartialPermission.value = isPartial
        if (granted && changed) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            mediaRepository.refreshMediaLibrary()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOptionSelected(option: SortOption) {
        viewModelScope.launch {
            preferencesRepository.setSortOption(option)
        }
    }

    fun playItems(items: List<LocalMediaItem>, startIndex: Int = 0, resumeIfVideo: Boolean = true) {
        if (items.isEmpty()) return
        val target = items.getOrElse(startIndex) { items.first() }
        viewModelScope.launch {
            val startPos = if (
                resumeIfVideo &&
                target.mediaType == MediaType.VIDEO &&
                userPrefs.value.resumeVideoPlayback
            ) {
                val saved = mediaRepository.getSavedPosition(target.id)
                if (target.durationMs > 0 && saved < target.durationMs - 3000L) saved else 0L
            } else 0L
            playbackManager.playQueue(items, startIndex, startPos)
        }
    }

    fun openExternalUri(uri: Uri, onVideoOpened: () -> Unit = {}) {
        viewModelScope.launch {
            val item = mediaRepository.resolveExternalMedia(uri)
            playbackManager.playQueue(listOf(item), 0, 0L)
            if (item.mediaType == MediaType.VIDEO) {
                onVideoOpened()
            }
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekTo(posMs: Long) = playbackManager.seekTo(posMs)
    fun seekRelative(deltaMs: Long) = playbackManager.seekRelative(deltaMs)
    fun skipNext() = playbackManager.skipNext()
    fun skipPrevious() = playbackManager.skipPrevious()

    fun cycleRepeatMode() {
        val next = playbackManager.cycleRepeatMode()
        viewModelScope.launch { preferencesRepository.setRepeatMode(next) }
    }

    fun toggleShuffle() {
        val next = playbackManager.toggleShuffle()
        viewModelScope.launch { preferencesRepository.setShuffleEnabled(next) }
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
        viewModelScope.launch { preferencesRepository.setDefaultPlaybackSpeed(speed) }
    }

    fun setSleepTimer(minutes: Int) = playbackManager.setSleepTimer(minutes)

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch { mediaRepository.toggleFavorite(mediaId) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { mediaRepository.createPlaylist(name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { mediaRepository.deletePlaylist(playlistId) }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { mediaRepository.addTrackToPlaylist(playlistId, mediaId) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { mediaRepository.removeTrackFromPlaylist(playlistId, mediaId) }
    }

    fun setDarkThemeMode(mode: Int) {
        viewModelScope.launch { preferencesRepository.setDarkThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColors(enabled) }
    }

    fun setResumeVideoPlayback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setResumeVideoPlayback(enabled) }
    }

    fun setAutoSkipCorrupted(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSkipCorrupted(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { mediaRepository.clearPlaybackHistory() }
    }

    override fun onCleared() {
        playbackManager.release()
        super.onCleared()
    }

    class Factory(private val application: XPlayerApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                appContext = application.applicationContext,
                mediaRepository = application.mediaRepository,
                preferencesRepository = application.preferencesRepository
            ) as T
        }
    }
}
