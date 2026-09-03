package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.Holiday
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays WHERE semesterId = :semesterId ORDER BY startDate ASC")
    fun getHolidaysForSemester(semesterId: Long): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays WHERE semesterId = :semesterId ORDER BY startDate ASC")
    suspend fun getHolidaysForSemesterSync(semesterId: Long): List<Holiday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holiday: Holiday): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<Holiday>)

    @Update
    suspend fun update(holiday: Holiday)

    @Delete
    suspend fun delete(holiday: Holiday)

    @Query("DELETE FROM holidays WHERE semesterId = :semesterId")
    suspend fun deleteHolidaysForSemester(semesterId: Long)

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()
}
