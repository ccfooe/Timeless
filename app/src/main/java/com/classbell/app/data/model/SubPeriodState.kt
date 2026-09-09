package com.classbell.app.data.model

import java.time.LocalTime

enum class SubPeriodKind {
    IN_CLASS,  // 正在上课中
    BREAK,     // 课间休息中
    PRE_CLASS, // 课前准备（开课前30分钟内）
    FINISHED   // 课程全部结束
}

data class SubPeriodState(
    val kind: SubPeriodKind,
    val periodOrder: Int,            // 当前节次 (如 1 或 5)
    val periodIndexInCourse: Int,    // 当前大节内的第几小节 (如 1/4)
    val totalPeriodsInCourse: Int,   // 本大节包含几小节 (如 4)
    val periodLabel: String,         // "第1节" / "第5节"
    val nextPeriodLabel: String? = null, // 课间时下一小节标签 "第2节" / "第6节"
    val startTime: LocalTime,        // 本小节或课间的开始时间
    val endTime: LocalTime,          // 本小节或课间的结束时间 (倒计时目标)
    val progress: Float,             // 本小节或课间的真实进度 0f..1f
    val remainingSeconds: Long,      // 剩余秒数
    val remainingMinutes: Long,      // 剩余分钟数
    val isBreak: Boolean = false,    // 是否处于课间
    val breakTitle: String? = null   // "课间休息" 或 "大课间休息"
)
