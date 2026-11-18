package com.oneledger.samsung.data.ledger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 主收支資料庫（ledger.db）：存放收支明細、循環規則、信用卡回饋設定、待確認重複與通知去敏紀錄。
 * 與借還款資料庫（debts.db）完全隔離。
 */
@Database(
    entities = [
        TransactionEntity::class,
        RecurringRuleEntity::class,
        CreditCardEntity::class,
        PendingDuplicateEntity::class,
        NotificationLogEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringDao(): RecurringDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun pendingDuplicateDao(): PendingDuplicateDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getInstance(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
