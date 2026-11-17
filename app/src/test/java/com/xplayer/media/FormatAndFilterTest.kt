package com.xplayer.media

import android.net.Uri
import android.os.Build
import com.xplayer.media.domain.model.LocalMediaItem
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.domain.model.SortOption
import com.xplayer.media.util.FormatUtils
import com.xplayer.media.util.PermissionHelper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FormatAndFilterTest {

    private fun sampleItem(
        id: Long,
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        durationMs: Long = 120_000L,
        sizeBytes: Long = 4_000_000L,
        dateAddedSec: Long = 1000L
    ) = LocalMediaItem(
        id = id,
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        folderName = "Music",
        mimeType = "audio/mpeg",
        dateAddedSec = dateAddedSec,
        mediaType = MediaType.AUDIO
    )

    @Test
    fun `formatDuration handles zero, negative, minutes and hours`() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:00", FormatUtils.formatDuration(-500L))
        assertEquals("03:25", FormatUtils.formatDuration(205_000L))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000L))
    }

    @Test
    fun `sanitizeMetadata replaces unknown and blank values with fallback`() {
        assertEquals("Unknown Artist", FormatUtils.sanitizeMetadata("<unknown>", "Unknown Artist"))
        assertEquals("Unknown Artist", FormatUtils.sanitizeMetadata("   ", "Unknown Artist"))
        assertEquals("Unknown Artist", FormatUtils.sanitizeMetadata(null, "Unknown Artist"))
        assertEquals("Daft Punk", FormatUtils.sanitizeMetadata(" Daft Punk ", "Unknown Artist"))
    }

    @Test
    fun `filterAndSort handles empty library without crashing`() {
        val result = FormatUtils.filterAndSort(emptyList(), "query", SortOption.TITLE_ASC)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterAndSort filters by query and sorts correctly`() {
        val items = listOf(
            sampleItem(1, "Zebra Song", artist = "Alpha", durationMs = 100_000L, dateAddedSec = 10L),
            sampleItem(2, "Acoustic Sunrise", artist = "Beta", durationMs = 300_000L, dateAddedSec = 50L),
            sampleItem(3, "Midnight Jazz", artist = "Alpha", durationMs = 200_000L, dateAddedSec = 30L)
        )

        val byTitleAsc = FormatUtils.filterAndSort(items, "", SortOption.TITLE_ASC)
        assertEquals(listOf(2L, 3L, 1L), byTitleAsc.map { it.id })

        val byDurationDesc = FormatUtils.filterAndSort(items, "", SortOption.DURATION_DESC)
        assertEquals(listOf(2L, 3L, 1L), byDurationDesc.map { it.id })

        val filteredByArtist = FormatUtils.filterAndSort(items, "alpha", SortOption.TITLE_ASC)
        assertEquals(listOf(3L, 1L), filteredByArtist.map { it.id })
    }

    @Test
    fun `PermissionHelper returns SDK-appropriate permissions`() {
        val api34Perms = PermissionHelper.getRequiredMediaPermissions(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        assertEquals(3, api34Perms.size)

        val api33Perms = PermissionHelper.getRequiredMediaPermissions(Build.VERSION_CODES.TIRAMISU)
        assertEquals(2, api33Perms.size)

        val api30Perms = PermissionHelper.getRequiredMediaPermissions(Build.VERSION_CODES.R)
        assertArrayEquals(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), api30Perms)
    }
}
