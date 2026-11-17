package com.xplayer.media.domain.model

import android.net.Uri

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class SortOption(val label: String) {
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    DATE_DESC("Newest First"),
    DURATION_DESC("Longest First"),
    SIZE_DESC("Largest Size")
}

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folderName: String,
    val mimeType: String,
    val dateAddedSec: Long,
    val mediaType: MediaType,
    val artworkUri: Uri? = null,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L
) {
    val safeTitle: String
        get() = title.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Untitled Media"

    val safeArtist: String
        get() = artist.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"

    val safeAlbum: String
        get() = album.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Album"
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class PermissionRequired(val isPartialGrant: Boolean = false) : LibraryUiState
    data object Empty : LibraryUiState
    data class Error(val message: String) : LibraryUiState
    data class Content(
        val audioItems: List<LocalMediaItem>,
        val videoItems: List<LocalMediaItem>,
        val folders: Map<String, List<LocalMediaItem>>,
        val favorites: List<LocalMediaItem>,
        val searchQuery: String,
        val sortOption: SortOption
    ) : LibraryUiState
}
