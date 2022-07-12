package com.starbowproj.musicplayer.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = arrayOf(Playlist::class, PlaylistMusic::class), version = 3, exportSchema = false)
abstract class PlaylistRoomHelper: RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistMusicDao(): PlaylistMusicDao

    companion object {
        private var helper: PlaylistRoomHelper? = null

        //PlaylistRoomHelper 객체를 반환, helper가 null일 경우 객체 생성 후 반환
        fun getHelper(context: Context): PlaylistRoomHelper {
            if (helper == null) {
                synchronized(PlaylistRoomHelper::class.java) {
                    helper = Room.databaseBuilder(context, PlaylistRoomHelper::class.java, "playlist")
                        .allowMainThreadQueries()
                        .addMigrations(PLAYLIST_MIGRATION_1_2, PLAYLIST_MIGRATION_2_3) //DB 버전이 오를 경우 변경 사항을 반영하는 Migration을 추가
                        .build()
                }
            }
            return helper!!
        }
    }
}

//DB의 버전이 1에서 2로 올랐을 때 이전 경로
val PLAYLIST_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) { //이전 방식은 이 메서드 안에 정의한다.
        db.execSQL("""
            CREATE TABLE new_playlist_music (
                id TEXT NOT NULL,
                playlistNo INTEGER NOT NULL,
                "order" INTEGER NOT NULL DEFAULT 0,
                title TEXT,
                artist TEXT,
                albumId TEXT,
                duration INTEGER,
                PRIMARY KEY(id, playlistNo)
            )
            """.trimIndent())
        db.execSQL("""
            INSERT INTO new_playlist_music (id, playlistNo, title, artist, albumId, duration)
            SELECT id, playlistNo, title, artist, albumId, duration FROM playlist_music
            """.trimIndent())
        db.execSQL("DROP TABLE playlist_music")
        db.execSQL("ALTER TABLE new_playlist_music RENAME TO playlist_music")
    }
}

//DB의 버전이 2에서 3으로 올랐을 때 이전 경로
val PLAYLIST_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) { //이전 방식은 이 메서드 안에 정의한다.
        db.execSQL("ALTER TABLE playlist_music RENAME COLUMN 'order' TO music_order")
    }
}

