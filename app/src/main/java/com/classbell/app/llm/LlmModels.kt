package com.classbell.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.0
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>
)

@Serializable
data class OpenAiContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: OpenAiImageUrl? = null
)

@Serializable
data class OpenAiImageUrl(
    val url: String
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiResponseMessage
)

@Serializable
data class OpenAiResponseMessage(
    val role: String = "assistant",
    val content: String = ""
)

@Serializable
data class OpenAiError(
    val message: String = "",
    val code: String? = null
)

// Timetable Parsed Schemas
@Serializable
data class ParsedCourseWeeks(
    val from: Int? = 1,
    val to: Int? = 18,
    val pattern: String? = "all" // all, odd, even
)

@Serializable
data class ParsedCourseItem(
    val name: String = "",
    val code: String? = null,
    val dayOfWeek: Int = 1, // 1-7
    val periodStart: Int = 1, // 1-10
    val periodEnd: Int = 1,   // 1-10
    val weeks: ParsedCourseWeeks? = ParsedCourseWeeks(),
    val campus: String? = null,
    val room: String? = null,
    val className: String? = null,
    val studentCount: Int? = null,
    val teacher: String? = null,
    val nature: String? = null,
    val examType: String? = null
)

// Academic Calendar Parsed Schemas
@Serializable
data class ParsedWeekRange(
    val from: Int = 1,
    val to: Int = 18
)

@Serializable
data class ParsedHolidayItem(
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val type: String? = "rest" // rest, makeup
)

@Serializable
data class ParsedMakeupWorkday(
    val date: String = "",
    val followsWeekday: Int? = null
)

@Serializable
data class ParsedCalendarResult(
    val semesterName: String = "",
    val week1Monday: String = "2026-08-24",
    val teachingWeeks: ParsedWeekRange = ParsedWeekRange(2, 18),
    val examWeeks: ParsedWeekRange? = null,
    val practiceWeeks: ParsedWeekRange? = null,
    val holidays: List<ParsedHolidayItem> = emptyList(),
    val makeupWorkdays: List<ParsedMakeupWorkday> = emptyList(),
    val notes: List<String> = emptyList()
)

// Smart Affair Parsed Schema
@Serializable
data class ParsedAffair(
    val title: String = "",
    val date: String? = null,
    val time: String? = null,
    val place: String? = null,
    val category: String? = null,
    val note: String? = null
)

// Period Table (作息时间表) Parsed Schema
@Serializable
data class ParsedPeriodSlotItem(
    val order: Int = 1,
    val label: String? = null,
    val startTime: String = "", // "HH:mm"
    val endTime: String = ""    // "HH:mm"
)

// Universal All-in-One Ingestion Schema
@Serializable
data class ParsedShiftOverride(
    val targetDate: String = "",       // "YYYY-MM-DD"
    val kind: String = "SHIFT",        // "SHIFT" | "CANCEL" | "COURSE_ADJUST"
    val followsWeekday: Int? = null,   // 1-7 (1=周一, 7=周日)
    val courseName: String? = null,
    val targetPeriodStart: Int? = null,
    val targetPeriodEnd: Int? = null,
    val targetRoom: String? = null,
    val note: String? = null
)

@Serializable
data class UniversalIngestResult(
    val primaryIntent: String = "AFFAIR", // SHIFT_OVERRIDE, AFFAIR, COURSE_TABLE, CALENDAR, PERIOD_TABLE, MIXED
    val summary: String = "",
    val shiftOverride: ParsedShiftOverride? = null,
    val shiftOverrides: List<ParsedShiftOverride> = emptyList(),
    val affairs: List<ParsedAffair> = emptyList(),
    val courses: List<ParsedCourseItem> = emptyList(),
    val calendar: ParsedCalendarResult? = null,
    val periodSlots: List<ParsedPeriodSlotItem> = emptyList()
) {
    /**
     * 合并多条调课规则与旧版单数字段，避免信息丢失并去重
     */
    fun resolvedShiftOverrides(): List<ParsedShiftOverride> {
        val list = mutableListOf<ParsedShiftOverride>()
        list.addAll(shiftOverrides)
        if (list.isEmpty() && shiftOverride != null && shiftOverride.targetDate.isNotBlank()) {
            list.add(shiftOverride)
        }
        return list.filter { it.targetDate.isNotBlank() }
            .distinctBy { "${it.targetDate}_${it.kind}_${it.followsWeekday}_${it.courseName}" }
    }

    /**
     * 规范化并提炼待办事项列表：
     * 1. 过滤空标题
     * 2. 严格控制硬性上限为最多 3 个事项（策略C：避免公文碎片化）
     * 3. 按照标题与日期去重
     */
    fun resolvedAffairs(): List<ParsedAffair> {
        return affairs
            .filter { it.title.isNotBlank() }
            .distinctBy { "${it.title}_${it.date}_${it.time}" }
            .take(3)
    }
}

