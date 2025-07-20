package com.nuerovent.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AuditEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audit_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
