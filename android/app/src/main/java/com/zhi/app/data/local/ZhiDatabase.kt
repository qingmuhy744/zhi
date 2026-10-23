package com.zhi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zhi.app.data.model.PushMessage

@Database(entities = [PushMessage::class], version = 2, exportSchema = false)
abstract class ZhiDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ZhiDatabase? = null

        fun getInstance(context: Context): ZhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZhiDatabase::class.java,
                    "zhi_messages.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
