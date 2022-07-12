package com.starbowproj.musicplayer.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query

@Dao
interface PlaylistMusicDao {
    //해당 플레이리스트에 속하는 모든 음원 얻기
    //@Query에 매개변수를 적용할 때 ":변수명" 형태로 적용하면 된다.
    @Query("select * from playlist_music where playlistNo = :playlistNo order by music_order asc")
    fun getAllMusic(playlistNo: Long): List<PlaylistMusic>

    @Query("select count(*) from playlist_music where playlistNo = :playlistNo")
    fun getNumMusic(playlistNo: Long): Int

    //음원 추가
    @Insert(onConflict = IGNORE)
    fun insertMusic(musicTable: PlaylistMusic)

    //음원 삭제
    @Delete
    fun deleteMusic(musicTable: PlaylistMusic)

    //해당 플레이리스트에 속하는 모든 음원 삭제
    @Query("delete from playlist_music where playlistNo = :playlistNo")
    fun deleteAllPlaylistMusic(playlistNo: Long)

    @Query("update playlist_music set music_order = :after where playlistNo = :playlistNo and id = :musicId")
    fun changeOrder(playlistNo: Long, musicId: String, after: Int)
}