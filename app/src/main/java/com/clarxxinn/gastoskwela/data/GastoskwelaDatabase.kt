package com.clarxxinn.gastoskwela.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Allowance::class],
    version = 1,
    exportSchema = false
)
abstract class GastoskwelaDatabase : RoomDatabase() {

    abstract fun allowanceDao(): AllowanceDao

    companion object {

        @Volatile
        private var INSTANCE: GastoskwelaDatabase? = null

        fun getDatabase(context: Context): GastoskwelaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastoskwelaDatabase::class.java,
                    "gastoskwela_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}