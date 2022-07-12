package com.starbowproj.musicplayer.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = arrayOf(PlayCount::class), version = 2, exportSchema = false)
abstract class PlayCountRoomHelper: RoomDatabase() {
    abstract fun playCountDao(): PlayCountDao

    companion object {
        private var helper: PlayCountRoomHelper? = null

        fun getHelper(context: Context): PlayCountRoomHelper {
            if(helper == null) {
                helper = Room.databaseBuilder(context, PlayCountRoomHelper::class.java, "ranking")
                    .allowMainThreadQueries()
                    .addMigrations(PLAYCOUNT_MIGRATION_1_2)
                    .build()
            }

            return helper!!
        }
    }
}

val PLAYCOUNT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE play_count ADD COLUMN duration INTEGER")
    }
}