package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.Affair
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AffairDao {

    @Query("SELECT * FROM affairs ORDER BY date ASC, time ASC")
    fun getAllAffairs(): Flow<List<Affair>>

    @Query("SELECT * FROM affairs ORDER BY date ASC, time ASC")
    suspend fun getAllAffairsSync(): List<Affair>

    @Query("SELECT * FROM affairs WHERE date = :date ORDER BY time ASC")
    fun getAffairsForDate(date: LocalDate): Flow<List<Affair>>

    @Query("SELECT * FROM affairs WHERE date = :date ORDER BY time ASC")
    suspend fun getAffairsForDateSync(date: LocalDate): List<Affair>

    @Query("SELECT * FROM affairs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getAffairsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Affair>>

    @Query("SELECT * FROM affairs WHERE id = :id")
    suspend fun getAffairById(id: Long): Affair?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(affair: Affair): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(affairs: List<Affair>)

    @Update
    suspend fun update(affair: Affair)

    @Delete
    suspend fun delete(affair: Affair)

    @Query("DELETE FROM affairs")
    suspend fun deleteAll()
}
