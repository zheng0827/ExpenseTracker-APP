package com.oneledger.samsung.data.debt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 獨立的借還款 SQLite 資料庫（debts.db）：
 * 僅作個人往來備忘紀錄，不與主帳本收支合併。
 */
@Database(
    entities = [DebtEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DebtDatabase : RoomDatabase() {
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: DebtDatabase? = null

        fun getInstance(context: Context): DebtDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DebtDatabase::class.java,
                    "debts.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
