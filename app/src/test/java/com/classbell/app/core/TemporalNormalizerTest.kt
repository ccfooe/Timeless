package com.classbell.app.core

import com.classbell.app.llm.ParsedAffair
import com.classbell.app.llm.UniversalIngestResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TemporalNormalizerTest {

    private val thursday = LocalDate.of(2026, 10, 15) // 2026-10-15 is Thursday

    @Test
    fun testNormalizeThisSaturdayWhenLlmReturnedToday() {
        val rawText = "@所有人 请各位老师，本周周六20:00之前完成参培统计表，统计时间区间为：2025年9月1日-2026年8月31日。【金山文档】 https://www.kdocs.cn/l/cirH1TTFchqg"
        val rawAffair = ParsedAffair(
            title = "完成参培统计表填报",
            date = "2026-10-15", // Model incorrectly defaulted to today
            time = "20:00",
            place = "金山文档 | WPS云文档",
            category = "AFFAIR"
        )
        val initialResult = UniversalIngestResult(
            primaryIntent = "AFFAIR",
            summary = "完成参培统计表填报",
            affairs = listOf(rawAffair)
        )

        val normalized = TemporalNormalizer.normalize(initialResult, rawText, thursday)
        val affair = normalized.resolvedAffairs().first()

        // Saturday of 2026-10-15 week is 2026-10-17
        assertEquals("2026-10-17", affair.date)
        assertEquals("20:00", affair.time)
    }

    @Test
    fun testNormalizeThisSaturdayWhenDateIsNull() {
        val rawText = "本周六20:00前完成参培统计表"
        val rawAffair = ParsedAffair(
            title = "完成参培统计表",
            date = null, // Model returned null date
            time = "20:00"
        )
        val initialResult = UniversalIngestResult(
            primaryIntent = "AFFAIR",
            affairs = listOf(rawAffair)
        )

        val normalized = TemporalNormalizer.normalize(initialResult, rawText, thursday)
        val affair = normalized.resolvedAffairs().first()

        assertEquals("2026-10-17", affair.date)
    }

    @Test
    fun testNormalizeTomorrowAndDayAfterTomorrow() {
        // 1. Tomorrow
        val textTomorrow = "明天下午3点在302开会"
        val resultTomorrow = TemporalNormalizer.normalize(
            UniversalIngestResult(affairs = listOf(ParsedAffair(title = "开会", date = null, time = "15:00"))),
            textTomorrow,
            thursday
        )
        assertEquals("2026-10-16", resultTomorrow.resolvedAffairs().first().date)

        // 2. Day after tomorrow
        val textAfter = "后天晚上8点交作业"
        val resultAfter = TemporalNormalizer.normalize(
            UniversalIngestResult(affairs = listOf(ParsedAffair(title = "交作业", date = null, time = "20:00"))),
            textAfter,
            thursday
        )
        assertEquals("2026-10-17", resultAfter.resolvedAffairs().first().date)
    }

    @Test
    fun testNormalizeNextFriday() {
        val text = "请在下周五 17:00 前提交项目申报材料"
        val result = TemporalNormalizer.normalize(
            UniversalIngestResult(affairs = listOf(ParsedAffair(title = "提交项目申报材料", date = "2026-10-15", time = "17:00"))),
            text,
            thursday
        )
        // 2026-10-15 is Thursday -> Next Friday is 2026-10-23
        assertEquals("2026-10-23", result.resolvedAffairs().first().date)
    }

    @Test
    fun testNormalizeMonthEnd() {
        val text = "请于本月底前核对发票并提交报销单"
        val result = TemporalNormalizer.normalize(
            UniversalIngestResult(affairs = listOf(ParsedAffair(title = "核对发票并提交报销单", date = null))),
            text,
            thursday
        )
        // October has 31 days
        assertEquals("2026-10-31", result.resolvedAffairs().first().date)
    }

    @Test
    fun testExplicitAbsoluteDatePreserved() {
        val text = "11月11日举行秋季运动会开幕式"
        val result = TemporalNormalizer.normalize(
            UniversalIngestResult(affairs = listOf(ParsedAffair(title = "秋季运动会开幕式", date = "2026-11-11"))),
            text,
            thursday
        )
        // Explicit future date should not be overridden
        assertEquals("2026-11-11", result.resolvedAffairs().first().date)
    }
}
