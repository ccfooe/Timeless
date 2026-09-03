package com.classbell.app.data.repository

import com.classbell.app.data.local.AffairDao
import com.classbell.app.data.model.Affair
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AffairRepository(private val affairDao: AffairDao) {

    val allAffairs: Flow<List<Affair>> = affairDao.getAllAffairs()

    suspend fun getAllAffairsSync(): List<Affair> = affairDao.getAllAffairsSync()

    fun getAffairsForDate(date: LocalDate): Flow<List<Affair>> = affairDao.getAffairsForDate(date)

    suspend fun getAffairsForDateSync(date: LocalDate): List<Affair> = affairDao.getAffairsForDateSync(date)

    fun getAffairsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Affair>> =
        affairDao.getAffairsBetweenDates(startDate, endDate)

    suspend fun getAffairById(id: Long): Affair? = affairDao.getAffairById(id)

    suspend fun insertAffair(affair: Affair): Long = affairDao.insert(affair)

    suspend fun insertAll(affairs: List<Affair>) = affairDao.insertAll(affairs)

    suspend fun updateAffair(affair: Affair) = affairDao.update(affair)

    suspend fun deleteAffair(affair: Affair) = affairDao.delete(affair)
}
