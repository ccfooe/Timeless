package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val week1Monday: LocalDate,
    val teachingFrom: Int = 2,
    val teachingTo: Int = 18,
    val examFrom: Int? = 19,
    val examTo: Int? = 20,
    val practiceFrom: Int? = 21,
    val practiceTo: Int? = 21,
    val notes: String = "",
    val isActive: Boolean = false
)
