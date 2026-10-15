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
        SchoolPayment::class,
        SchoolPaymentTransaction::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GastoskwelaDatabase : RoomDatabase() {

    abstract fun allowanceDao(): AllowanceDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun schoolPaymentDao(): SchoolPaymentDao

    abstract fun schoolPaymentTransactionDao():
            SchoolPaymentTransactionDao

    companion object {

        @Volatile
        private var INSTANCE: GastoskwelaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountCentavos INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        date TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS school_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amountCentavos INTEGER NOT NULL,
                        paidCentavos INTEGER NOT NULL,
                        dueDate TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                // Add payment method to existing expenses.
                db.execSQL(
                    """
                    ALTER TABLE expenses
                    ADD COLUMN paymentMethod TEXT NOT NULL
                    DEFAULT 'Unspecified'
                    """.trimIndent()
                )

                // Preserve the original cumulative paid amount.
                db.execSQL(
                    """
                    ALTER TABLE school_payments
                    ADD COLUMN legacyPaidCentavos INTEGER NOT NULL
                    DEFAULT 0
                    """.trimIndent()
                )

                // Record historical paid amounts as opening balances.
                // Do not invent dates for historical payments.
                db.execSQL(
                    """
                    UPDATE school_payments
                    SET legacyPaidCentavos = paidCentavos
                    """.trimIndent()
                )

                // Create a table for future dated payments.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS
                    school_payment_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        schoolPaymentId INTEGER NOT NULL,
                        amountCentavos INTEGER NOT NULL,
                        paymentDate TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        FOREIGN KEY(schoolPaymentId)
                        REFERENCES school_payments(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Index for efficient payment history queries.
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_school_payment_transactions_schoolPaymentId
                    ON school_payment_transactions(schoolPaymentId)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(
            context: Context
        ): GastoskwelaDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastoskwelaDatabase::class.java,
                    "gastoskwela_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}