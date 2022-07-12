package com.starbowproj.musicplayer.room

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE

@Dao
interface PlayCountDao {
    @Query("select * from play_count order by playCount desc")
    fun getAllPlayCount(): MutableList<PlayCount>

    @Query("update play_count set playCount = playCount + 1 where musicId = :musicId")
    fun countUp(musicId: String)

    @Insert(onConflict = IGNORE)
    fun insertPlayCount(playCount: PlayCount)

    @Delete
    fun deletePlayCount(playCount: PlayCount)
}