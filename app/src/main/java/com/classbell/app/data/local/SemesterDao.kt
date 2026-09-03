package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {

    @Query("SELECT * FROM semesters ORDER BY id DESC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun getActiveSemester(): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSemesterSync(): Semester?

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: Long): Semester?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(semester: Semester): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(semesters: List<Semester>)

    @Update
    suspend fun update(semester: Semester)

    @Delete
    suspend fun delete(semester: Semester)

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun clearActiveSemester()

    @Query("UPDATE semesters SET isActive = 1 WHERE id = :id")
    suspend fun setActiveSemester(id: Long)

    @Query("DELETE FROM semesters")
    suspend fun deleteAll()
}
