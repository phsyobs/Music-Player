package com.starbowproj.musicplayer.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query

@Dao
interface PlaylistDao {
    //생성된 모든 플레이리스트 목록 얻기
    @Query("select * from playlist")
    fun getAllPlaylist(): MutableList<Playlist>

    //플레이리스트 생성
    @Insert(onConflict = IGNORE)
    fun insertPlaylist(playList: Playlist)

    //플레이리스트 삭제(※포함되어 있는 음원은 여기서 삭제가 이루어지지 않음)
    @Delete
    fun delete(playList: Playlist)
}