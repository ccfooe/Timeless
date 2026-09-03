package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class HolidayType {
    REST,   // 休
    MAKEUP  // 补 / 班
}

@Serializable
@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val type: HolidayType = HolidayType.REST,
    val followsWeekday: Int? = null // 补课日按周几课表 (1-7), null=未指定
)
