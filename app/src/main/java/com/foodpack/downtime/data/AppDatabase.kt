package com.foodpack.downtime.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DowntimeEntry::class, HandoverEntry::class, BreakEntry::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downtimeDao(): DowntimeDao
    abstract fun handoverDao(): HandoverDao
    abstract fun breakDao(): BreakDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "downtime.db"
                )
                    // v1 -> v2 added the handover table and two new columns
                    // on downtime_entries; v2 -> v3 added the same
                    // sheetRowCreated/pendingDelete pair to handover_entries
                    // for Handover edit/delete support. There's no
                    // user-facing data worth preserving through a schema
                    // change on this internal tool, so just rebuild rather
                    // than hand-write a Migration.
                    // v3 -> v4: peopleAffected column + break_entries table.
                    // v4 -> v5: photoPath / photoUrl on downtime_entries.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
