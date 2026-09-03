package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
enum class AffairCategory {
    MEETING,  // 会议
    EXAM,     // 考试
    AFFAIR,   // 事务
    OTHER     // 其他
}

@Serializable
enum class ReminderChoice {
    NONE,     // 不提醒
    AT_TIME,  // 准时
    MIN10,    // 提前10分钟
    HOUR1,    // 提前1小时
    DAY1      // 提前1天
}

@Serializable
@Entity(tableName = "affairs")
data class Affair(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class)
    val time: LocalTime? = null,
    val place: String? = null,
    val note: String? = null,
    val category: AffairCategory = AffairCategory.AFFAIR,
    val reminder: ReminderChoice = ReminderChoice.NONE
)
