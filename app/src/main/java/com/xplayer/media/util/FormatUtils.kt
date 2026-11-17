package com.xplayer.media.util

import com.xplayer.media.domain.model.LocalMediaItem
import com.xplayer.media.domain.model.SortOption
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun sanitizeMetadata(rawValue: String?, fallback: String): String {
        val trimmed = rawValue?.trim()
        return if (trimmed.isNullOrEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) {
            fallback
        } else {
            trimmed
        }
    }

    fun filterAndSort(
        items: List<LocalMediaItem>,
        query: String,
        sortOption: SortOption
    ): List<LocalMediaItem> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val filtered = if (cleanQuery.isEmpty()) {
            items
        } else {
            items.filter { item ->
                item.safeTitle.lowercase(Locale.ROOT).contains(cleanQuery) ||
                    item.safeArtist.lowercase(Locale.ROOT).contains(cleanQuery) ||
                    item.safeAlbum.lowercase(Locale.ROOT).contains(cleanQuery) ||
                    item.folderName.lowercase(Locale.ROOT).contains(cleanQuery)
            }
        }

        return when (sortOption) {
            SortOption.TITLE_ASC -> filtered.sortedBy { it.safeTitle.lowercase(Locale.ROOT) }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.safeTitle.lowercase(Locale.ROOT) }
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.dateAddedSec }
            SortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }
}
