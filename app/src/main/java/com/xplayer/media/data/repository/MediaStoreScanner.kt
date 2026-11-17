package com.xplayer.media.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.xplayer.media.domain.model.LocalMediaItem
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    suspend fun scanAudioLibrary(): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocalMediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Audio.Media.DATA
            }
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else -1

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol) ?: "audio/*"
                    val dateAdded = cursor.getLong(dateCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val folder = if (bucketCol >= 0) {
                        cursor.getString(bucketCol)
                    } else {
                        File(path).parentFile?.name
                    } ?: "Music"

                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId)
                    } else null

                    results += LocalMediaItem(
                        id = id,
                        uri = contentUri,
                        title = FormatUtils.sanitizeMetadata(rawTitle, File(path).nameWithoutExtension.ifBlank { "Untitled Audio" }),
                        artist = FormatUtils.sanitizeMetadata(rawArtist, "Unknown Artist"),
                        album = FormatUtils.sanitizeMetadata(rawAlbum, "Unknown Album"),
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        folderName = folder.ifBlank { "Music" },
                        mimeType = mimeType,
                        dateAddedSec = dateAdded,
                        mediaType = MediaType.AUDIO,
                        artworkUri = artworkUri
                    )
                }
            }
        }
        results
    }

    suspend fun scanVideoLibrary(): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocalMediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        val selection = "${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol) ?: "video/*"
                    val dateAdded = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val folder = if (bucketCol >= 0) {
                        cursor.getString(bucketCol)
                    } else {
                        File(path).parentFile?.name
                    } ?: "Videos"

                    results += LocalMediaItem(
                        id = id,
                        uri = contentUri,
                        title = FormatUtils.sanitizeMetadata(rawTitle, File(path).nameWithoutExtension.ifBlank { "Untitled Video" }),
                        artist = FormatUtils.sanitizeMetadata(rawArtist, "Local Video"),
                        album = FormatUtils.sanitizeMetadata(rawAlbum, folder.ifBlank { "Videos" }),
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        folderName = folder.ifBlank { "Videos" },
                        mimeType = mimeType,
                        dateAddedSec = dateAdded,
                        mediaType = MediaType.VIDEO,
                        artworkUri = contentUri,
                        width = width,
                        height = height
                    )
                }
            }
        }
        results
    }

    suspend fun resolveExternalUri(uri: Uri): LocalMediaItem = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = uri.lastPathSegment ?: "External Media"
        var sizeBytes = 0L

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = c.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = c.getLong(sizeIdx).coerceAtLeast(0L)
                }
            }
        }

        val mimeType = resolver.getType(uri) ?: if (
            displayName.endsWith(".mp4", true) ||
            displayName.endsWith(".mkv", true) ||
            displayName.endsWith(".webm", true)
        ) "video/*" else "audio/*"

        var title = displayName.substringBeforeLast('.')
        var artist = "External File"
        var album = "Direct Open"
        var durationMs = 0L
        var hasVideo = mimeType.startsWith("video", ignoreCase = true)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = FormatUtils.sanitizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                title
            )
            artist = FormatUtils.sanitizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                artist
            )
            album = FormatUtils.sanitizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                album
            )
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val videoTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (videoTrack != null) {
                hasVideo = videoTrack == "yes"
            }
        } catch (_: Exception) {
            // Corrupted header or missing metadata; fallback values are preserved
        } finally {
            runCatching { retriever.release() }
        }

        LocalMediaItem(
            id = uri.toString().hashCode().toLong(),
            uri = uri,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            folderName = "External",
            mimeType = mimeType,
            dateAddedSec = System.currentTimeMillis() / 1000L,
            mediaType = if (hasVideo) MediaType.VIDEO else MediaType.AUDIO,
            artworkUri = if (hasVideo) uri else null
        )
    }
}
