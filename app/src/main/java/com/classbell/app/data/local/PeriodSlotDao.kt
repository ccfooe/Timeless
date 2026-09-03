package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.PeriodSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodSlotDao {

    @Query("SELECT * FROM period_slots ORDER BY `order` ASC")
    fun getAllPeriodSlots(): Flow<List<PeriodSlot>>

    @Query("SELECT * FROM period_slots ORDER BY `order` ASC")
    suspend fun getAllPeriodSlotsSync(): List<PeriodSlot>

    @Query("SELECT * FROM period_slots WHERE `order` = :order LIMIT 1")
    suspend fun getPeriodByOrder(order: Int): PeriodSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(periodSlot: PeriodSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(periodSlots: List<PeriodSlot>)

    @Update
    suspend fun update(periodSlot: PeriodSlot)

    @Delete
    suspend fun delete(periodSlot: PeriodSlot)

    @Query("DELETE FROM period_slots")
    suspend fun deleteAll()
}
