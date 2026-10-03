package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.Override
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface OverrideDao {

    @Query("SELECT * FROM overrides WHERE semesterId = :semesterId")
    fun getOverridesForSemester(semesterId: Long): Flow<List<Override>>

    @Query("SELECT * FROM overrides WHERE semesterId = :semesterId")
    suspend fun getOverridesForSemesterSync(semesterId: Long): List<Override>

    @Query("SELECT * FROM overrides WHERE semesterId = :semesterId AND date = :date")
    fun getOverridesForDateFlow(semesterId: Long, date: LocalDate): Flow<List<Override>>

    @Query("SELECT * FROM overrides WHERE semesterId = :semesterId AND date = :date")
    suspend fun getOverridesForDateSync(semesterId: Long, date: LocalDate): List<Override>

    @Query("SELECT * FROM overrides WHERE semesterId = :semesterId AND date = :date LIMIT 1")
    suspend fun getOverrideForDate(semesterId: Long, date: LocalDate): Override?

    @Query("DELETE FROM overrides WHERE semesterId = :semesterId AND date = :date")
    suspend fun deleteOverridesForDate(semesterId: Long, date: LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(override: Override): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(overrides: List<Override>)

    @Update
    suspend fun update(override: Override)

    @Delete
    suspend fun delete(override: Override)

    @Query("DELETE FROM overrides WHERE semesterId = :semesterId")
    suspend fun deleteOverridesForSemester(semesterId: Long)

    @Query("DELETE FROM overrides")
    suspend fun deleteAll()
}
