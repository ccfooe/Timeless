package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class WeekPattern {
    ALL,  // 每周
    ODD,  // 单周
    EVEN  // 双周
}

@Serializable
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val name: String,
    val code: String? = null,
    val dayOfWeek: Int, // 1=Monday .. 7=Sunday
    val periodStart: Int, // e.g. 1
    val periodEnd: Int,   // e.g. 4
    val weekFrom: Int,    // e.g. 2
    val weekTo: Int,      // e.g. 17
    val weekPattern: WeekPattern = WeekPattern.ALL,
    val weekExclusions: List<Int> = emptyList(), // Manual exclusions
    val campus: String? = "金牛校区",
    val room: String,     // e.g. "贡院401"
    val className: String, // e.g. "25级广播电视编导03班"
    val studentCount: Int = 0, // e.g. 49
    val nature: String? = "必修", // 必修 / 选修
    val examType: String? = "考试", // 考试 / 考查
    val color: String = "#FF4D6D" // Hex color string
)
