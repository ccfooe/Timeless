package com.classbell.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classbell.app.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, periodStart")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek, periodStart")
    fun getCoursesForSemester(semesterId: Long): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek, periodStart")
    suspend fun getCoursesForSemesterSync(semesterId: Long): List<Course>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    @Update
    suspend fun update(course: Course)

    @Delete
    suspend fun delete(course: Course)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesForSemester(semesterId: Long)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
