package com.classbell.app.core

import com.classbell.app.llm.ParsedAffair
import com.classbell.app.llm.ParsedShiftOverride
import com.classbell.app.llm.UniversalIngestResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Ultra-fast local rule-based parser (< 2ms) for typical Chinese notices,
 * affairs, meetings, and shift announcements.
 *
 * Provides instantaneous response (matching ColorOS Breeno / system assistant)
 * for daily short texts, while transparently falling back to cloud LLM for
 * complex full-semester timetables, school calendars, and images.
 */
object FastLocalParser {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(rawText: String, refDate: LocalDate = LocalDate.now()): UniversalIngestResult? = tryParse(rawText, refDate)

    fun tryParse(rawText: String, refDate: LocalDate = LocalDate.now()): UniversalIngestResult? {
        val trimmed = rawText.trim()
        if (trimmed.isBlank() || trimmed.length > 250) {
            return null
        }

        // =========================================================================
        // Step 1 & 2: Complexity_Scanning & Smart_Routing_Gate
        // (meta_parser_guardrails.yaml: URLs, collaborative markers, multi-time-domain)
        // =========================================================================

        // 1. External URLs or web links -> leave to LLM for deep semantic understanding
        val hasUrl = trimmed.contains("http://", ignoreCase = true) ||
                trimmed.contains("https://", ignoreCase = true) ||
                trimmed.contains("www.", ignoreCase = true) ||
                trimmed.contains(".cn/", ignoreCase = true) ||
                trimmed.contains(".com/", ignoreCase = true) ||
                trimmed.contains("kdocs", ignoreCase = true) ||
                trimmed.contains("wjx.cn", ignoreCase = true) ||
                trimmed.contains("forms.", ignoreCase = true) ||
                trimmed.contains("feishu", ignoreCase = true)
        if (hasUrl) return null

        // 2. Collaborative forms, online surveys, questionnaires -> leave to LLM
        val isCollaborativeSurvey = trimmed.contains("金山文档") ||
                trimmed.contains("WPS") ||
                trimmed.contains("腾讯文档") ||
                trimmed.contains("问卷星") ||
                trimmed.contains("飞书文档") ||
                trimmed.contains("调查表") ||
                trimmed.contains("统计表") ||
                trimmed.contains("多维表格") ||
                trimmed.contains("报名链接") ||
                trimmed.contains("填报") ||
                trimmed.contains("在线文档")
        if (isCollaborativeSurvey) return null

        // 3. Multi-time-domain & Statistical intervals (confuses single-event local regex)
        val hasStatisticalInterval = trimmed.contains("时间区间") ||
                trimmed.contains("统计区间") ||
                trimmed.contains("统计范围") ||
                trimmed.contains("区间为") ||
                trimmed.contains("统计时间") ||
                trimmed.contains("统计周期") ||
                trimmed.contains("起止时间") ||
                trimmed.contains("统计跨度")
        if (hasStatisticalInterval) return null

        // Multi-time-domain: detects mixed time ranges or multiple conflicting dates
        if (detectMultiTimeDomain(trimmed)) return null

        // If it looks like a complex table or whole week schedule, leave to LLM
        if (trimmed.contains("|") && trimmed.count { it == '|' } > 3) return null
        if (trimmed.count { it == '\n' } > 4) return null
        if (trimmed.contains("课程名称") && trimmed.contains("教室") && trimmed.contains("周次")) return null

        // =========================================================================
        // Step 3: Noise_Cleaning_Pipe (Apply pre-filter to strip boilerplate prefixes)
        // =========================================================================
        val cleanText = cleanNoisePrefixes(trimmed)
        if (cleanText.isBlank()) return null

        // 1. Try to parse Shift / Cancel Announcement
        parseShiftOverride(cleanText, refDate)?.let { return it }

        // 2. Try to parse Affair / Meeting / Exam
        parseAffair(cleanText, refDate)?.let { return it }

        return null
    }

    /**
     * Noise_Cleaning_Pipe: Pre-filter boilerplate prefixes (\u2005/@全体成员/请各位老师)
     */
    fun cleanNoisePrefixes(raw: String): String {
        return raw
            .replace(Regex("@(?:所有人|全体成员|群成员|all|Everyone)[\\u2005\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^[【\\[](?:通知|紧急通知|重要通知|温馨提示|公告|日程提醒)[】\\]][：:\\s]*"), "")
            .replace(Regex("^(?:通知|紧急通知|重要通知|温馨提示|公告|日程提醒)[：:\\s]+"), "")
            .replace(Regex("^(?:请各位老师|请各位同学|各位老师|各位同学|请各部门|各部门同事|请大家|请)[，,。！!？?：:；;\\s]*"), "")
            .trim()
    }

    /**
     * Complexity_Scanning: Detects multi-time-domain (mixed date ranges and deadline keywords)
     */
    private fun detectMultiTimeDomain(text: String): Boolean {
        // e.g. "2024-01-01 至 2025-12-31" or "2024年...至...2025年"
        val rangeRegex = Regex("""[0-9]{4}[年\-/.][0-9]{1,2}[月\-/.][0-9]{1,2}[日号]?\s*(?:至|到|~|-)\s*[0-9]{4}[年\-/.][0-9]{1,2}[月\-/.][0-9]{1,2}[日号]?""")
        if (rangeRegex.containsMatchIn(text)) return true

        // Count distinct year or full date occurrences (e.g. 2024, 2025)
        val yearPattern = Regex("""\b(?:202[0-9])年?""")
        val yearMatches = yearPattern.findAll(text).map { it.value }.toSet()
        if (yearMatches.size >= 2) return true

        // If contains deadline keywords AND multiple dates
        val hasDeadlineKeyword = text.contains("之前") || text.contains("前完成") || text.contains("前提交") || text.contains("截止")
        val dateMatches = Regex("""[0-9]{1,2}[月\-/.][0-9]{1,2}[日号]?""").findAll(text).count()
        if (hasDeadlineKeyword && dateMatches >= 2) return true

        return false
    }

    private fun parseShiftOverride(text: String, refDate: LocalDate): UniversalIngestResult? {
        val isShiftKeyword = text.contains("调课") ||
                text.contains("换课") ||
                text.contains("行课") ||
                text.contains("补课") ||
                text.contains("调整至") ||
                text.contains("调至") ||
                text.contains("调整到") ||
                text.contains("改至") ||
                text.contains("移至") ||
                text.contains("改到") ||
                text.contains("按原周") ||
                text.contains("按周") ||
                text.contains("照周") ||
                text.contains("停课")

        if (!isShiftKeyword) return null

        // 1. High-priority university notice pattern: "A调整至B" / "A调至B" / "A调整到B"
        // e.g. "9月30日（周三）的课程调整至9月27日（周日）"
        val adjustRegex = Regex("""(?:调整至|调至|改至|调整到|移至|改到)\s*""", RegexOption.IGNORE_CASE)
        val adjustMatch = adjustRegex.find(text)
        if (adjustMatch != null) {
            val textBefore = text.substring(0, adjustMatch.range.first)
            val textAfter = text.substring(adjustMatch.range.last + 1)

            val dateAfter = extractDate(textAfter, refDate)
            if (dateAfter != null) {
                // targetDate is the execution date (B)
                val dateBefore = extractDate(textBefore, refDate)
                val weekdayPattern = Pattern.compile("(?:周|星期|礼拜)([一二三四五六日天1-7])")
                val weekdayMatcher = weekdayPattern.matcher(textBefore)
                val followsWeekday = if (weekdayMatcher.find()) {
                    val wdStr = weekdayMatcher.group(1) ?: ""
                    cnWeekdayToInt(wdStr) ?: (dateBefore?.dayOfWeek?.value ?: 1)
                } else {
                    dateBefore?.dayOfWeek?.value ?: 1
                }

                val targetOverride = ParsedShiftOverride(
                    kind = "SHIFT",
                    targetDate = dateAfter.toString(),
                    followsWeekday = followsWeekday,
                    note = if (dateBefore != null) "补${dateBefore.monthValue}月${dateBefore.dayOfMonth}日(周${TimeUtils.getWeekdayNameChinese(followsWeekday)})课程" else text
                )

                // 成对双向联动：自动在原调离日生成停课标记，静默避让并保留调离追溯证据
                val overridesList = mutableListOf(targetOverride)
                if (dateBefore != null) {
                    val sourceOverride = ParsedShiftOverride(
                        kind = "CANCEL",
                        targetDate = dateBefore.toString(),
                        note = "课程已调整至${dateAfter.monthValue}月${dateAfter.dayOfMonth}日(周${TimeUtils.getWeekdayNameChinese(dateAfter.dayOfWeek.value)})"
                    )
                    overridesList.add(sourceOverride)
                }

                val summaryText = if (dateBefore != null) {
                    "检测到成对调课：${dateAfter} 补周${TimeUtils.getWeekdayNameChinese(followsWeekday)}课表；${dateBefore} 课程已调出停课"
                } else {
                    "检测到调课通知：${dateAfter} 执行周${TimeUtils.getWeekdayNameChinese(followsWeekday)}课表行课"
                }

                return UniversalIngestResult(
                    primaryIntent = "SHIFT_OVERRIDE",
                    summary = summaryText,
                    shiftOverride = targetOverride,
                    shiftOverrides = overridesList
                )
            }
        }

        val targetDate = extractDate(text, refDate) ?: return null

        // 2. Check if CANCEL
        if (text.contains("停课") || text.contains("取消上课") || text.contains("放假停课")) {
            val override = ParsedShiftOverride(
                kind = "CANCEL",
                targetDate = targetDate.toString(),
                note = text
            )
            return UniversalIngestResult(
                primaryIntent = "SHIFT_OVERRIDE",
                summary = "检测到停课通知：${targetDate} 全天停课",
                shiftOverride = override,
                shiftOverrides = listOf(override)
            )
        }

        // 3. Check target weekday: "按(原)?周[一二三四五六日天1-7]" or "补周[一二三四五六日天1-7]"
        val shiftPattern = Pattern.compile("(?:按原?|照|补|按|转为|改为)(?:周|星期|礼拜)([一二三四五六日天1-7])")
        val matcher = shiftPattern.matcher(text)
        if (matcher.find()) {
            val weekdayCn = matcher.group(1) ?: return null
            val targetWeekday = cnWeekdayToInt(weekdayCn) ?: return null

            val override = ParsedShiftOverride(
                kind = "SHIFT",
                targetDate = targetDate.toString(),
                followsWeekday = targetWeekday,
                note = text
            )

            return UniversalIngestResult(
                primaryIntent = "SHIFT_OVERRIDE",
                summary = "检测到调课通知：${targetDate} 按周${TimeUtils.getWeekdayNameChinese(targetWeekday)}课表行课",
                shiftOverride = override,
                shiftOverrides = listOf(override)
            )
        }

        return null
    }

    private fun parseAffair(text: String, refDate: LocalDate): UniversalIngestResult? {
        val dateMatch = extractDateWithRange(text, refDate)
        val timeMatch = extractTimeWithRange(text)

        // If neither date nor time could be extracted, cannot reliably form an affair
        if (dateMatch == null && timeMatch == null) {
            return null
        }

        val targetDate = dateMatch?.first ?: refDate
        val targetTime = timeMatch?.first

        // Location extraction
        val locationMatch = extractLocationWithRange(text)
        val location = locationMatch?.first

        // Deduce category
        val category = when {
            text.contains("会") || text.contains("研讨") || text.contains("座谈") ||
                    text.contains("答辩") || text.contains("宣讲") || text.contains("例会") ||
                    text.contains("评审") || text.contains("讨论") -> "MEETING"
            text.contains("考") || text.contains("测验") || text.contains("期末") ||
                    text.contains("期中") || text.contains("竞赛") -> "EXAM"
            else -> "AFFAIR"
        }

        // Clean title
        var title = text
        dateMatch?.second?.let { range ->
            title = title.removeRangeSafely(range.first, range.second)
        }
        timeMatch?.second?.let { range ->
            title = title.removeRangeSafely(range.first, range.second)
        }
        locationMatch?.second?.let { range ->
            title = title.removeRangeSafely(range.first, range.second)
        }

        title = cleanTitle(title, category)

        return UniversalIngestResult(
            primaryIntent = "AFFAIR",
            summary = "检测到日程事项：$title (${targetDate}${targetTime?.let { " $it" } ?: ""})",
            affairs = listOf(
                ParsedAffair(
                    title = title,
                    date = targetDate.toString(),
                    time = targetTime?.format(TIME_FORMATTER),
                    place = location,
                    category = category,
                    note = text
                )
            )
        )
    }

    private fun extractDate(text: String, refDate: LocalDate): LocalDate? {
        return extractDateWithRange(text, refDate)?.first
    }

    private fun extractDateWithRange(text: String, refDate: LocalDate): Pair<LocalDate, Pair<Int, Int>>? {
        // 1. Explicit YYYY-MM-DD or YYYY年MM月DD日
        val ymdPattern = Pattern.compile("([0-9]{4})[年\\-\\/.]([0-9]{1,2})[月\\-\\/.]([0-9]{1,2})[日号]?")
        val ymdMatcher = ymdPattern.matcher(text)
        if (ymdMatcher.find()) {
            val y = ymdMatcher.group(1)?.toIntOrNull() ?: refDate.year
            val m = ymdMatcher.group(2)?.toIntOrNull() ?: 1
            val d = ymdMatcher.group(3)?.toIntOrNull() ?: 1
            runCatching { LocalDate.of(y, m, d) }.getOrNull()?.let {
                return Pair(it, Pair(ymdMatcher.start(), ymdMatcher.end()))
            }
        }

        // 2. Month-Day: MM月DD日 / MM.DD / MM-DD
        val mdPattern = Pattern.compile("([0-9]{1,2})[月\\-\\/.]([0-9]{1,2})[日号]?")
        val mdMatcher = mdPattern.matcher(text)
        if (mdMatcher.find()) {
            val m = mdMatcher.group(1)?.toIntOrNull() ?: 1
            val d = mdMatcher.group(2)?.toIntOrNull() ?: 1
            runCatching { LocalDate.of(refDate.year, m, d) }.getOrNull()?.let {
                return Pair(it, Pair(mdMatcher.start(), mdMatcher.end()))
            }
        }

        // 3. Relative words: 今天, 明天, 后天, 大后天, 昨天
        val relWords = listOf(
            "大后天" to 3,
            "后天" to 2,
            "明天" to 1,
            "明晚" to 1,
            "今天" to 0,
            "今早" to 0,
            "今晚" to 0,
            "昨天" to -1,
            "昨晚" to -1
        )
        for ((word, days) in relWords) {
            val idx = text.indexOf(word)
            if (idx >= 0) {
                return Pair(refDate.plusDays(days.toLong()), Pair(idx, idx + word.length))
            }
        }

        // 4. Weekday: 下周X, 本周X, 这周X, 周X, 星期X, 礼拜X
        val weekPattern = Pattern.compile("(本周|这周|下周|下下周|周|星期|礼拜)([一二三四五六日天1-7])")
        val weekMatcher = weekPattern.matcher(text)
        if (weekMatcher.find()) {
            val prefix = weekMatcher.group(1) ?: ""
            val dayStr = weekMatcher.group(2) ?: ""
            val targetDow = cnWeekdayToInt(dayStr) ?: 1

            val currentDow = refDate.dayOfWeek.value
            val date = when (prefix) {
                "下周" -> refDate.plusWeeks(1).with(DayOfWeek.of(targetDow))
                "下下周" -> refDate.plusWeeks(2).with(DayOfWeek.of(targetDow))
                "本周", "这周" -> refDate.with(DayOfWeek.of(targetDow))
                else -> {
                    // "周五" / "星期五" -> If targetDow >= currentDow, this week; else next week
                    if (targetDow >= currentDow) {
                        refDate.with(DayOfWeek.of(targetDow))
                    } else {
                        refDate.plusWeeks(1).with(DayOfWeek.of(targetDow))
                    }
                }
            }
            return Pair(date, Pair(weekMatcher.start(), weekMatcher.end()))
        }

        return null
    }

    private fun extractTimeWithRange(text: String): Pair<LocalTime, Pair<Int, Int>>? {
        // 1. Chinese period time: (上午|早上|早晨|中午|下午|傍晚|晚上|夜间)?\s*([0-9]{1,2})[:：点]([0-9]{1,2}|半)?分?
        val cnTimePattern = Pattern.compile("(上午|早上|早晨|早间|中午|下午|傍晚|晚上|晚间|夜间|今晚|明晚)?\\s*([0-9]{1,2})[:：点]([0-9]{1,2}|半)?分?")
        val matcher = cnTimePattern.matcher(text)
        if (matcher.find()) {
            val period = matcher.group(1)
            val rawHour = matcher.group(2)?.toIntOrNull() ?: 0
            val minStr = matcher.group(3)

            var hour = rawHour
            val isPm = period != null && (period.contains("下午") || period.contains("晚上") || period.contains("晚") || period.contains("夜") || period.contains("傍晚"))
            if (isPm && hour < 12) {
                hour += 12
            } else if (period == "中午" && hour < 11) {
                hour += 12
            }

            val minute = when {
                minStr == "半" -> 30
                minStr.isNullOrBlank() -> 0
                else -> minStr.toIntOrNull() ?: 0
            }

            if (hour in 0..23 && minute in 0..59) {
                return Pair(LocalTime.of(hour, minute), Pair(matcher.start(), matcher.end()))
            }
        }

        // 2. Standard 24h: 14:30, 09:00
        val stdTimePattern = Pattern.compile("([0-1]?[0-9]|2[0-3])[:：]([0-5][0-9])")
        val stdMatcher = stdTimePattern.matcher(text)
        if (stdMatcher.find()) {
            val h = stdMatcher.group(1)?.toIntOrNull() ?: 0
            val m = stdMatcher.group(2)?.toIntOrNull() ?: 0
            return Pair(LocalTime.of(h, m), Pair(stdMatcher.start(), stdMatcher.end()))
        }

        return null
    }

    private fun extractLocationWithRange(text: String): Pair<String, Pair<Int, Int>>? {
        val locPattern = Pattern.compile("(?:在|地点[：: ]*|于|去|到)\\s*([A-Za-z0-9\\u4e00-\\u9fa5]{0,15}?(?:教室|教研室|会议室|报告厅|大礼堂|体育馆|运动场|操场|图书馆|机房|实验室|中心|[0-9]{3,4}室?|[A-Z][0-9]{3,4}|(?:[楼馆厅室](?:[A-Za-z0-9\\-]+室?)?)))")
        val matcher = locPattern.matcher(text)
        if (matcher.find()) {
            val loc = matcher.group(1)?.trim()
            if (!loc.isNullOrBlank()) {
                return Pair(loc, Pair(matcher.start(), matcher.end()))
            }
        }
        return null
    }

    private fun cleanTitle(raw: String, category: String): String {
        var clean = raw
            .replace(Regex("@(所有人|全体成员|群成员)[\\u2005\\s]*"), " ")
            .replace(Regex("^(请各位老师|请各位同学|各位老师|各位同学|请大家|请|通知|提醒|记得)[，,。！!？?：:；;\\s]*"), " ")
            .replace(Regex("[，,。！!？?：:；;、（）()]+"), " ")
            .replace("在", " ")
            .replace("地点", " ")
            .replace("通知", " ")
            .replace("提醒", " ")
            .replace("记得", " ")
            .replace("各老师", " ")
            .replace("各位老师", " ")
            .replace("各位同学", " ")
            .replace("请各位老师", " ")
            .replace("请各位同学", " ")
            .replace("请大家", " ")
            .replace("请准时", " ")
            .replace("准时参加", " ")
            .replace("参加", " ")
            .replace("去", " ")
            .trim()

        val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val candidate = tokens.firstOrNull { it.length >= 2 } ?: clean

        return if (candidate.length >= 2) {
            candidate
        } else {
            when (category) {
                "MEETING" -> "会议待办"
                "EXAM" -> "考试安排"
                else -> "待办事项"
            }
        }
    }

    private fun cnWeekdayToInt(cn: String): Int? {
        return when (cn) {
            "一", "1" -> 1
            "二", "2" -> 2
            "三", "3" -> 3
            "四", "4" -> 4
            "五", "5" -> 5
            "六", "6" -> 6
            "日", "天", "7" -> 7
            else -> null
        }
    }

    private fun String.removeRangeSafely(start: Int, end: Int): String {
        return if (start in 0 until length && end in start..length) {
            this.removeRange(start, end)
        } else {
            this
        }
    }
}
