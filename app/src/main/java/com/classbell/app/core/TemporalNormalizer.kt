package com.classbell.app.core

import com.classbell.app.llm.ParsedAffair
import com.classbell.app.llm.UniversalIngestResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.regex.Pattern

/**
 * 确定性时态校准器 (Deterministic Temporal Normalizer)
 *
 * 专门解决大模型或本地解析在处理“本周周六”、“本周六”、“明天”、“后天”、“下周五”、“月底”等
 * 相对日期时发生的心算偏差、漏填(date=null)或错误退化为今天(refDate)的缺陷。
 *
 * 遵循 Ubiquitous Language 与纯领域无副作用设计原则。
 */
object TemporalNormalizer {

    fun normalize(
        result: UniversalIngestResult,
        rawText: String,
        refDate: LocalDate = LocalDate.now()
    ): UniversalIngestResult {
        if (result.affairs.isEmpty()) return result

        val normalizedAffairs = result.affairs.map { affair ->
            normalizeAffair(affair, rawText, refDate)
        }

        return result.copy(affairs = normalizedAffairs)
    }

    fun normalizeAffair(
        affair: ParsedAffair,
        rawText: String,
        refDate: LocalDate = LocalDate.now()
    ): ParsedAffair {
        val searchCorpus = listOfNotNull(rawText, affair.title, affair.note).joinToString("\n")
        val deducedDate = deduceRelativeDate(searchCorpus, refDate) ?: return affair

        val currentDateStr = affair.date?.trim()
        val isDateMissingOrEmpty = currentDateStr.isNullOrBlank()
        val isDateDefaultedToToday = currentDateStr == refDate.toString()

        // 仅在日期缺失、或者错误降级为基准日(今天)而文本明确指向非今天的未来相对日期时，进行确定性校准
        return if (isDateMissingOrEmpty || (isDateDefaultedToToday && deducedDate != refDate)) {
            affair.copy(date = deducedDate.toString())
        } else {
            affair
        }
    }

    /**
     * 从文本中提取高优先级的相对时态标记并推算精确公历日期
     */
    fun deduceRelativeDate(text: String, refDate: LocalDate): LocalDate? {
        // 1. 周末 / 本周周X / 本周X / 这周周X / 这周X / 下周周X / 下周X / 下下周X / 周X / 星期X / 礼拜X
        val weekPattern = Pattern.compile("((?:下下周|下周|本周|这周)(?:\\s*(?:周|星期|礼拜))?|(?:周|星期|礼拜))\\s*([一二三四五六日天1-7]|末)")
        val weekMatcher = weekPattern.matcher(text)
        if (weekMatcher.find()) {
            val prefix = weekMatcher.group(1) ?: ""
            val dayStr = weekMatcher.group(2) ?: ""
            val targetDow = if (dayStr == "末") 6 else (cnWeekdayToInt(dayStr) ?: 6)

            val currentDow = refDate.dayOfWeek.value
            return when {
                prefix.startsWith("下下周") -> refDate.plusWeeks(2).with(DayOfWeek.of(targetDow))
                prefix.startsWith("下周") -> refDate.plusWeeks(1).with(DayOfWeek.of(targetDow))
                prefix.startsWith("本周") || prefix.startsWith("这周") -> refDate.with(DayOfWeek.of(targetDow))
                else -> {
                    // "周六" / "星期六" / "礼拜六"
                    if (targetDow >= currentDow) {
                        refDate.with(DayOfWeek.of(targetDow))
                    } else {
                        refDate.plusWeeks(1).with(DayOfWeek.of(targetDow))
                    }
                }
            }
        }

        // 2. 相对天数：大后天, 后天, 明天, 次日, 今天
        if (text.contains("大后天")) return refDate.plusDays(3)
        if (text.contains("后天")) return refDate.plusDays(2)
        if (text.contains("明天") || text.contains("明晚") || text.contains("次日")) return refDate.plusDays(1)
        if (text.contains("今天") || text.contains("今晚") || text.contains("今日")) return refDate

        // 3. 月末 / 月底 / 本月底 / 本月末
        if (text.contains("月底") || text.contains("月末") || text.contains("本月底") || text.contains("本月末")) {
            return refDate.withDayOfMonth(refDate.lengthOfMonth())
        }

        return null
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
}
