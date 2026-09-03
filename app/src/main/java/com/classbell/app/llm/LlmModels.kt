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
    val from: Int = 1,
    val to: Int = 18,
    val pattern: String = "all" // all, odd, even
)

@Serializable
data class ParsedCourseItem(
    val name: String,
    val code: String? = null,
    val dayOfWeek: Int, // 1-7
    val periodStart: Int, // 1-10
    val periodEnd: Int,   // 1-10
    val weeks: ParsedCourseWeeks = ParsedCourseWeeks(),
    val campus: String? = "金牛校区",
    val room: String = "",
    val className: String = "",
    val studentCount: Int = 0,
    val nature: String? = "必修",
    val examType: String? = "考试"
)

// Academic Calendar Parsed Schemas
@Serializable
data class ParsedWeekRange(
    val from: Int,
    val to: Int
)

@Serializable
data class ParsedHolidayItem(
    val name: String,
    val startDate: String,
    val endDate: String,
    val type: String = "rest" // rest, makeup
)

@Serializable
data class ParsedMakeupWorkday(
    val date: String,
    val followsWeekday: Int? = null
)

@Serializable
data class ParsedCalendarResult(
    val semesterName: String = "",
    val week1Monday: String = "2026-08-24",
    val teachingWeeks: ParsedWeekRange = ParsedWeekRange(2, 18),
    val examWeeks: ParsedWeekRange? = ParsedWeekRange(19, 20),
    val practiceWeeks: ParsedWeekRange? = ParsedWeekRange(21, 21),
    val holidays: List<ParsedHolidayItem> = emptyList(),
    val makeupWorkdays: List<ParsedMakeupWorkday> = emptyList(),
    val notes: List<String> = emptyList()
)
