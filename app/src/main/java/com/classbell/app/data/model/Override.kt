package com.classbell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class OverrideKind {
    CANCEL,         // 停课 (整天停课 或 单门停课)
    SHIFT,          // 整天调课 (按周几课表克隆)
    COURSE_ADJUST   // 单门课程调课/补课/换课
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
    val followsWeekday: Int? = null, // e.g. 1-7 (整天调课所遵循的星期几)
    val courseId: Long? = null, // 单课调课/单课停课所关联的课程 ID
    val targetPeriodStart: Int? = null, // 目标起始节次 (单课调整)
    val targetPeriodEnd: Int? = null,   // 目标结束节次 (单课调整)
    val targetRoom: String? = null,     // 目标教室 (单课调整)
    val note: String? = null            // 备注说明 (如 "补周二第3-4节" / "临时换至502")
)
