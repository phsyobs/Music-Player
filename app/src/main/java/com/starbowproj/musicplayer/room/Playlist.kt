package com.starbowproj.musicplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

//생성된 플레이리스트의 목록을 저장할 테이블
//인텐트에 담을 수 있도록 Serializable을 상속 받음
@Entity(tableName = "playlist")
class Playlist: Serializable {
    @PrimaryKey
    @ColumnInfo
    var no: Long = 0

    @ColumnInfo
    var name: String = ""

    constructor(no: Long, name: String?) {
        this.no = no
        this.name = name ?: ""
    }
}