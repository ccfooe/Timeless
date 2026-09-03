package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
@Entity(tableName = "period_slots")
data class PeriodSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val order: Int, // 1..10
    val label: String, // e.g. "第1节"
    @Serializable(with = LocalTimeSerializer::class)
    val startTime: LocalTime,
    @Serializable(with = LocalTimeSerializer::class)
    val endTime: LocalTime,
    val campus: String? = null // null = 全校区通用
)
