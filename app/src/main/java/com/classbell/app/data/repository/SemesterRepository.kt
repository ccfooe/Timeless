package com.classbell.app.data.repository

import com.classbell.app.data.local.SemesterDao
import com.classbell.app.data.model.Semester
import kotlinx.coroutines.flow.Flow

class SemesterRepository(private val semesterDao: SemesterDao) {

    val allSemesters: Flow<List<Semester>> = semesterDao.getAllSemesters()
    val activeSemester: Flow<Semester?> = semesterDao.getActiveSemester()

    suspend fun getActiveSemesterSync(): Semester? = semesterDao.getActiveSemesterSync()

    suspend fun getSemesterById(id: Long): Semester? = semesterDao.getSemesterById(id)

    suspend fun insertSemester(semester: Semester): Long {
        if (semester.isActive) {
            semesterDao.clearActiveSemester()
        }
        return semesterDao.insert(semester)
    }

    suspend fun updateSemester(semester: Semester) {
        if (semester.isActive) {
            semesterDao.clearActiveSemester()
        }
        semesterDao.update(semester)
    }

    suspend fun deleteSemester(semester: Semester) = semesterDao.delete(semester)

    suspend fun setActiveSemester(id: Long) {
        semesterDao.clearActiveSemester()
        semesterDao.setActiveSemester(id)
    }
}
