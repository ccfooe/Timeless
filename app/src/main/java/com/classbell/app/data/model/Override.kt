package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class OverrideKind {
    CANCEL,  // 停课
    SHIFT    // 调课 (按周几课表)
}

@Serializable
@Entity(tableName = "overrides")
data class Override(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val kind: OverrideKind,
    val followsWeekday: Int? = null // e.g. 1-7
)
