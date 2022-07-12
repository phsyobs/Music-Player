package com.starbowproj.musicplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_count")
class PlayCount {
    @PrimaryKey
    @ColumnInfo
    var musicId: String = ""

    @ColumnInfo
    var musicTitle: String? = null

    @ColumnInfo
    var musicArtist: String? = null

    @ColumnInfo
    var albumId: String? = null

    @ColumnInfo
    var duration: Int? = null

    @ColumnInfo
    var playCount: Long = 0

    constructor(musicId: String,
                musicTitle: String?,
                musicArtist: String?,
                albumId: String?,
                duration: Int?,
                playCount: Long = 0L) {
        this.musicId = musicId
        this.musicTitle = musicTitle
        this.musicArtist = musicArtist
        this.albumId = albumId
        this.duration = duration
        this.playCount = playCount
    }
}