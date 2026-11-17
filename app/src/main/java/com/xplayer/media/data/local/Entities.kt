package com.xplayer.media.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val title: String,
    val mediaType: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0L,
    val name: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_media_cross_ref",
    primaryKeys = ["playlistId", "mediaId"],
    indices = [Index("mediaId")]
)
data class PlaylistMediaCrossRef(
    val playlistId: Long,
    val mediaId: Long,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

data class PlaylistWithMediaIds(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "playlistId",
        entity = PlaylistMediaCrossRef::class
    )
    val refs: List<PlaylistMediaCrossRef>
)
