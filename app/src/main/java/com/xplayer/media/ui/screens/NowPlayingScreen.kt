package com.xplayer.media.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.playback.PlaybackUiState
import com.xplayer.media.util.FormatUtils

@OptIn(UnstableApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    mediaController: MediaController?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onEnterPip: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    val current = playbackState.currentMedia ?: return
    val isVideo = current.mediaType == MediaType.VIDEO
    val localView = LocalView.current

    // Keep screen awake only when actively playing video
    DisposableEffect(isVideo, playbackState.isPlaying) {
        localView.keepScreenOn = isVideo && playbackState.isPlaying
        onDispose {
            localView.keepScreenOn = false
        }
    }

    var controlsLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isVideo) Color.Black else MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = if (isVideo) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (isVideo) "X Video Player" else "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isVideo) Color.White else MaterialTheme.colorScheme.onBackground
                )

                Row {
                    if (isVideo) {
                        IconButton(onClick = { controlsLocked = !controlsLocked }) {
                            Icon(
                                imageVector = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onToggleOrientation) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onEnterPip) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Center Artwork or ExoPlayer Video Surface with double-tap seek gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .pointerInput(controlsLocked) {
                        if (!controlsLocked) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2f) {
                                        onSeekRelative(-10_000L)
                                    } else {
                                        onSeekRelative(10_000L)
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isVideo && mediaController != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = mediaController
                                this.resizeMode = resizeMode
                            }
                        },
                        update = { view ->
                            if (view.player != mediaController) {
                                view.player = mediaController
                            }
                            view.resizeMode = resizeMode
                        },
                        onRelease = { view ->
                            // Prevent memory leaks by detaching Surface when Composable leaves composition
                            view.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (current.artworkUri != null) {
                            AsyncImage(
                                model = current.artworkUri,
                                contentDescription = current.safeTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    }
                }
            }

            if (!controlsLocked) {
                val fgColor = if (isVideo) Color.White else MaterialTheme.colorScheme.onBackground
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = current.safeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = fgColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${current.safeArtist} • ${current.safeAlbum}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = fgColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val duration = playbackState.durationMs.coerceAtLeast(1L)
                    val position = playbackState.currentPositionMs.coerceIn(0L, duration)
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(position),
                            style = MaterialTheme.typography.labelMedium,
                            color = fgColor
                        )
                        Text(
                            text = FormatUtils.formatDuration(playbackState.durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = fgColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary else fgColor
                            )
                        }
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = fgColor)
                        }
                        IconButton(onClick = { onSeekRelative(-10_000L) }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = fgColor)
                        }
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        IconButton(onClick = { onSeekRelative(10_000L) }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = fgColor)
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = fgColor)
                        }
                        IconButton(onClick = onCycleRepeat) {
                            val icon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Repeat Mode",
                                tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else fgColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Utility Chips: Speed, Sleep Timer, Aspect Ratio (for Video)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box {
                            AssistChip(
                                onClick = { showSpeedMenu = true },
                                label = { Text("${playbackState.playbackSpeed}x", color = fgColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = fgColor)
                                }
                            )
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    DropdownMenuItem(
                                        text = { Text("${spd}x") },
                                        onClick = {
                                            onSetSpeed(spd)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            val timerLabel = playbackState.sleepTimerRemainingMs?.let {
                                "Sleep: ${FormatUtils.formatDuration(it)}"
                            } ?: "Sleep Timer"
                            AssistChip(
                                onClick = { showSleepMenu = true },
                                label = { Text(timerLabel, color = fgColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = fgColor)
                                }
                            )
                            DropdownMenu(
                                expanded = showSleepMenu,
                                onDismissRequest = { showSleepMenu = false }
                            ) {
                                listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 60 to "60 min").forEach { (mins, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onSetSleepTimer(mins)
                                            showSleepMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (isVideo) {
                            AssistChip(
                                onClick = {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                label = { Text("Aspect Ratio", color = fgColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = fgColor)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
