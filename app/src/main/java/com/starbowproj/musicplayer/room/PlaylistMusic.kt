package com.starbowproj.musicplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity

//플레이리스트에 저장된 음원들을 저장하는 테이블
@Entity(tableName = "playlist_music", primaryKeys = ["id", "playlistNo"])
class PlaylistMusic {
    @ColumnInfo
    var id: String = ""

    @ColumnInfo
    var playlistNo: Long = 0

    @ColumnInfo(name = "music_order")
    var order: Int = 0

    @ColumnInfo
    var title: String? = null

    @ColumnInfo
    var artist: String? = null

    @ColumnInfo
    var albumId: String? = null

    @ColumnInfo
    var duration: Int? = null

    constructor(id: String, playlistNo: Long, order: Int, title: String?, artist: String?, albumId: String?, duration: Int?) {
        this.id = id
        this.playlistNo = playlistNo
        this.order = order
        this.title = title
        this.artist = artist
        this.albumId = albumId
        this.duration = duration
    }
}