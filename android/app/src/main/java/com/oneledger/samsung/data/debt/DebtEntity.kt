package com.oneledger.samsung.data.debt

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 獨立借還款實體（存放在專屬的 debts.db，結清與否完全不寫入主記帳 ledger.db，不影響收支統計）。
 */
@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val direction: String, // "lent" (別人欠我) | "borrowed" (我欠別人)
    val person: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val reason: String = "",
    val date: String,
    val dueDate: String = "",
    val settled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncPending: Boolean = true,
)
