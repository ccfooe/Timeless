package com.classbell.app.data.repository

import com.classbell.app.data.local.CourseDao
import com.classbell.app.data.model.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {

    fun getCoursesForSemester(semesterId: Long): Flow<List<Course>> =
        courseDao.getCoursesForSemester(semesterId)

    suspend fun getCoursesForSemesterSync(semesterId: Long): List<Course> =
        courseDao.getCoursesForSemesterSync(semesterId)

    suspend fun getCourseById(id: Long): Course? = courseDao.getCourseById(id)

    suspend fun insertCourse(course: Course): Long = courseDao.insert(course)

    suspend fun insertAll(courses: List<Course>) = courseDao.insertAll(courses)

    suspend fun updateCourse(course: Course) = courseDao.update(course)

    suspend fun deleteCourse(course: Course) = courseDao.delete(course)
}
