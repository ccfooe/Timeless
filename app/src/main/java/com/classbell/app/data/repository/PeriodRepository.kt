package com.classbell.app.data.repository

import com.classbell.app.data.local.PeriodSlotDao
import com.classbell.app.data.model.PeriodSlot
import kotlinx.coroutines.flow.Flow

class PeriodRepository(private val periodSlotDao: PeriodSlotDao) {

    val allPeriodSlots: Flow<List<PeriodSlot>> = periodSlotDao.getAllPeriodSlots()

    suspend fun getAllPeriodSlotsSync(): List<PeriodSlot> = periodSlotDao.getAllPeriodSlotsSync()

    suspend fun insertPeriodSlot(slot: PeriodSlot): Long = periodSlotDao.insert(slot)

    suspend fun insertAll(slots: List<PeriodSlot>) = periodSlotDao.insertAll(slots)

    suspend fun replaceAll(slots: List<PeriodSlot>) {
        periodSlotDao.deleteAll()
        periodSlotDao.insertAll(slots)
        com.classbell.app.ClassBellApp.cachedPeriodSlots = slots
    }

    suspend fun updatePeriodSlot(slot: PeriodSlot) = periodSlotDao.update(slot)

    suspend fun deletePeriodSlot(slot: PeriodSlot) = periodSlotDao.delete(slot)
}
