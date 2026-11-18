package com.oneledger.samsung.data.ledger

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // "expense" | "income"
    val description: String, // 單一正規化描述（顯示於首頁主標題）
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val photoUri: String? = null,
    val source: String = "manual", // "manual" | "notification" | "recurring"
    val sourceApp: String? = null,
    val creditCardId: String? = null,
    val isForeignTx: Boolean = false,
    val rewardPoints: Double = 0.0,
    val rewardUnit: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncPending: Boolean = true,
)

@Serializable
@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val description: String,
    val type: String, // "expense" | "income"
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val creditCardId: String? = null,
    val frequency: String = "monthly", // "monthly" | "weekly" | "yearly"
    val dayOfMonth: Int = 1,
    val active: Boolean = true,
    val lastPostedMonth: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncPending: Boolean = true,
)

@Serializable
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String, // 例如：國泰 CUBE、玉山 U Bear
    val rewardUnit: String, // 例如：小樹點、現金回饋、LINE POINTS、台新 Point
    val baseRatePercent: Double, // 一般消費回饋 %
    val bonusRatePercent: Double, // 指定分類加碼回饋 %
    val bonusCategoriesCsv: String, // 例如：餐飲,日常,交通
    val foreignRatePercent: Double = 3.0, // 海外消費回饋 %
    val foreignFeePercent: Double = 1.5, // 海外手續費 %
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncPending: Boolean = true,
)

@Entity(tableName = "pending_duplicates")
data class PendingDuplicateEntity(
    @PrimaryKey val id: String,
    val sourceApp: String,
    val description: String,
    val amount: Double,
    val category: String,
    val date: String,
    val time: String,
    val redactedText: String,
    val matchedExistingTitle: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val id: String,
    val appName: String,
    val status: String, // "recorded" | "pending_duplicate" | "deduplicated" | "ignored"
    val reason: String,
    val redactedText: String,
    val maskedCount: Int,
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
)
