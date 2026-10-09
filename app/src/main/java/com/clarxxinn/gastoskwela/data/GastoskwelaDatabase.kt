package com.clarxxinn.gastoskwela.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Allowance::class,
        Expense::class,
        SchoolPayment::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GastoskwelaDatabase : RoomDatabase() {

    abstract fun allowanceDao(): AllowanceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun schoolPaymentDao(): SchoolPaymentDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amountCentavos` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `school_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amountCentavos` INTEGER NOT NULL,
                        `paidCentavos` INTEGER NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: GastoskwelaDatabase? = null

        fun getDatabase(context: Context): GastoskwelaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastoskwelaDatabase::class.java,
                    "gastoskwela_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}