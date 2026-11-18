package com.oneledger.samsung.data.debt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE deletedAt IS NULL ORDER BY settled ASC, date DESC")
    fun observeAllActive(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE deletedAt IS NULL AND settled = 0")
    suspend fun getOpenDebts(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE syncPending = 1")
    suspend fun getPendingSync(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(debts: List<DebtEntity>)

    @Query("UPDATE debts SET deletedAt = :now, updatedAt = :now, syncPending = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE debts SET syncPending = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}
