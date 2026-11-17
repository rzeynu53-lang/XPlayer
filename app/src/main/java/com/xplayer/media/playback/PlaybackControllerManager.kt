package com.xplayer.media.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.xplayer.media.domain.model.LocalMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentMedia: LocalMediaItem? = null,
    val queue: List<LocalMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null,
    val playbackErrorMessage: String? = null
)

class PlaybackControllerManager(
    context: Context,
    private val onProgressUpdate: (LocalMediaItem, Long, Long) -> Unit = { _, _, _ -> }
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var autoSkipCorrupted: Boolean = true
    private val queueMap = mutableMapOf<String, LocalMediaItem>()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressPolling() else stopProgressPolling()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = mediaController ?: return
            _playbackState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = controller.duration.coerceAtLeast(0L),
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val controller = mediaController ?: return
            val localItem = mediaItem?.mediaId?.let { queueMap[it] }
            _playbackState.update {
                it.copy(
                    currentMedia = localItem,
                    currentIndex = controller.currentMediaItemIndex.coerceAtLeast(0),
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                    durationMs = controller.duration.coerceAtLeast(localItem?.durationMs ?: 0L),
                    playbackErrorMessage = null
                )
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val controller = mediaController
            val brokenTitle = _playbackState.value.currentMedia?.safeTitle ?: "Media file"
            val msg = "Cannot play \"$brokenTitle\" (${error.errorCodeName}). File may be corrupted or unsupported."
            _playbackState.update { it.copy(playbackErrorMessage = msg, isPlaying = false) }
            _errorEvents.tryEmit(msg)

            if (autoSkipCorrupted && controller != null && controller.hasNextMediaItem()) {
                scope.launch {
                    delay(800L)
                    controller.seekToNextMediaItem()
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    fun initialize() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        scope.launch {
            runCatching {
                val controller = future.await()
                mediaController = controller
                controller.addListener(playerListener)
                syncControllerState(controller)
            }
        }
    }

    private fun syncControllerState(controller: MediaController) {
        val currentId = controller.currentMediaItem?.mediaId
        val localItem = currentId?.let { queueMap[it] }
        _playbackState.update {
            it.copy(
                currentMedia = localItem ?: it.currentMedia,
                isPlaying = controller.isPlaying,
                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.coerceAtLeast(0L),
                repeatMode = controller.repeatMode,
                shuffleEnabled = controller.shuffleModeEnabled,
                playbackSpeed = controller.playbackParameters.speed
            )
        }
        if (controller.isPlaying) startProgressPolling()
    }

    fun setAutoSkipCorrupted(enabled: Boolean) {
        autoSkipCorrupted = enabled
    }

    fun playQueue(
        items: List<LocalMediaItem>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L
    ) {
        if (items.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, items.lastIndex)
        items.forEach { queueMap[it.id.toString()] = it }

        val media3Items = items.map { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.safeTitle)
                .setArtist(item.safeArtist)
                .setAlbumTitle(item.safeAlbum)
                .setArtworkUri(item.artworkUri)
                .setExtras(Bundle().apply {
                    putString("MEDIA_TYPE", item.mediaType.name)
                })
                .build()

            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uri)
                .setMimeType(item.mimeType)
                .setMediaMetadata(metadata)
                .build()
        }

        _playbackState.update {
            it.copy(
                queue = items,
                currentMedia = items[safeIndex],
                currentIndex = safeIndex,
                playbackErrorMessage = null
            )
        }

        val controller = mediaController
        if (controller != null) {
            controller.setMediaItems(media3Items, safeIndex, startPositionMs.coerceAtLeast(0L))
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            saveCurrentProgress()
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(clamped)
        _playbackState.update { it.copy(currentPositionMs = clamped) }
    }

    fun seekRelative(deltaMs: Long) {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + deltaMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun skipNext() {
        val controller = mediaController ?: return
        saveCurrentProgress()
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        saveCurrentProgress()
        if (controller.currentPosition > 4000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun cycleRepeatMode(): Int {
        val controller = mediaController ?: return Player.REPEAT_MODE_OFF
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
        return nextMode
    }

    fun toggleShuffle(): Boolean {
        val controller = mediaController ?: return false
        val next = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = next
        _playbackState.update { it.copy(shuffleEnabled = next) }
        return next
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        mediaController?.setPlaybackSpeed(clamped)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerRemainingMs = null) }
            return
        }
        val totalMs = minutes * 60_000L
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                _playbackState.update { it.copy(sleepTimerRemainingMs = remaining) }
                delay(1000L)
                remaining -= 1000L
            }
            if (isActive) {
                _playbackState.update { it.copy(sleepTimerRemainingMs = null) }
                saveCurrentProgress()
                mediaController?.pause()
            }
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tickCount = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    _playbackState.update {
                        it.copy(currentPositionMs = pos, durationMs = dur)
                    }
                    tickCount++
                    if (tickCount % 10 == 0) {
                        saveCurrentProgress()
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun saveCurrentProgress() {
        val controller = mediaController ?: return
        val item = _playbackState.value.currentMedia ?: return
        val pos = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.coerceAtLeast(item.durationMs)
        onProgressUpdate(item, pos, dur)
    }

    fun release() {
        saveCurrentProgress()
        stopProgressPolling()
        sleepTimerJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
