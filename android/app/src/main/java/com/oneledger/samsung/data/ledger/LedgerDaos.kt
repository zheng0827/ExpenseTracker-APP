package com.oneledger.samsung.data.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC, time DESC, updatedAt DESC")
    fun observeAllActive(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND date LIKE :monthPrefix ORDER BY date DESC, time DESC")
    suspend fun getByMonth(monthPrefix: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND date = :date")
    suspend fun getByDate(date: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncPending = 1")
    suspend fun getPendingSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransactionEntity>)

    @Query("UPDATE transactions SET deletedAt = :now, updatedAt = :now, syncPending = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET syncPending = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL ORDER BY dayOfMonth ASC")
    fun observeAllActive(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL AND active = 1")
    suspend fun getActiveRules(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE syncPending = 1")
    suspend fun getPendingSync(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<RecurringRuleEntity>)

    @Query("UPDATE recurring_rules SET deletedAt = :now, updatedAt = :now, syncPending = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE recurring_rules SET syncPending = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAllActive(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE deletedAt IS NULL")
    suspend fun getAllActive(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE syncPending = 1")
    suspend fun getPendingSync(): List<CreditCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CreditCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<CreditCardEntity>)

    @Query("UPDATE credit_cards SET deletedAt = :now, updatedAt = :now, syncPending = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE credit_cards SET syncPending = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}

@Dao
interface PendingDuplicateDao {
    @Query("SELECT * FROM pending_duplicates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingDuplicateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingDuplicateEntity)

    @Query("DELETE FROM pending_duplicates WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT 30")
    fun observeRecent(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NotificationLogEntity)
}
