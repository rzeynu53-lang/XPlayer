package com.xplayer.media.data.repository

import android.net.Uri
import com.xplayer.media.data.local.FavoriteEntity
import com.xplayer.media.data.local.MediaDao
import com.xplayer.media.data.local.PlaybackHistoryEntity
import com.xplayer.media.data.local.PlaylistEntity
import com.xplayer.media.data.local.PlaylistMediaCrossRef
import com.xplayer.media.data.local.PlaylistWithMediaIds
import com.xplayer.media.domain.model.LocalMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class MediaRepository(
    private val scanner: MediaStoreScanner,
    private val mediaDao: MediaDao
) {
    private val _rawAudioItems = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    private val _rawVideoItems = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val favoriteIdsFlow: Flow<Set<Long>> = combine(
        mediaDao.observeFavoriteIds()
    ) { (ids) -> ids.toSet() }

    val historyFlow: Flow<List<PlaybackHistoryEntity>> = mediaDao.observePlaybackHistory()

    val playlistsFlow: Flow<List<PlaylistWithMediaIds>> = mediaDao.observePlaylists()

    val audioItemsFlow: Flow<List<LocalMediaItem>> = combine(
        _rawAudioItems,
        favoriteIdsFlow,
        historyFlow
    ) { items, favs, history ->
        val historyMap = history.associateBy { it.mediaId }
        items.map { item ->
            item.copy(
                isFavorite = favs.contains(item.id),
                lastPositionMs = historyMap[item.id]?.lastPositionMs ?: 0L
            )
        }
    }

    val videoItemsFlow: Flow<List<LocalMediaItem>> = combine(
        _rawVideoItems,
        favoriteIdsFlow,
        historyFlow
    ) { items, favs, history ->
        val historyMap = history.associateBy { it.mediaId }
        items.map { item ->
            item.copy(
                isFavorite = favs.contains(item.id),
                lastPositionMs = historyMap[item.id]?.lastPositionMs ?: 0L
            )
        }
    }

    suspend fun refreshMediaLibrary() {
        _isScanning.value = true
        try {
            val audio = scanner.scanAudioLibrary()
            val video = scanner.scanVideoLibrary()
            _rawAudioItems.value = audio
            _rawVideoItems.value = video
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun resolveExternalMedia(uri: Uri): LocalMediaItem {
        return scanner.resolveExternalUri(uri)
    }

    suspend fun toggleFavorite(mediaId: Long) {
        if (mediaDao.isFavorite(mediaId)) {
            mediaDao.removeFavorite(mediaId)
        } else {
            mediaDao.addFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }

    suspend fun savePlaybackProgress(item: LocalMediaItem, positionMs: Long, durationMs: Long) {
        mediaDao.upsertPlaybackHistory(
            PlaybackHistoryEntity(
                mediaId = item.id,
                uriString = item.uri.toString(),
                title = item.safeTitle,
                mediaType = item.mediaType.name,
                lastPositionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(item.durationMs)
            )
        )
    }

    suspend fun getSavedPosition(mediaId: Long): Long {
        return mediaDao.getPlaybackHistoryForMedia(mediaId)?.lastPositionMs ?: 0L
    }

    suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().ifEmpty { "New Playlist" }
        return mediaDao.createPlaylist(PlaylistEntity(name = clean))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        mediaDao.clearPlaylistRefs(playlistId)
        mediaDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        mediaDao.addMediaToPlaylist(PlaylistMediaCrossRef(playlistId = playlistId, mediaId = mediaId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        mediaDao.removeMediaFromPlaylist(playlistId = playlistId, mediaId = mediaId)
    }

    suspend fun clearPlaybackHistory() {
        mediaDao.clearHistory()
    }
}
