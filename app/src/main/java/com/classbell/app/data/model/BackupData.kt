package com.classbell.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val semesters: List<Semester> = emptyList(),
    val courses: List<Course> = emptyList(),
    val periodSlots: List<PeriodSlot> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
    val affairs: List<Affair> = emptyList(),
    val overrides: List<Override> = emptyList()
)
