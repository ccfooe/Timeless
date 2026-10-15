package com.classbell.app.core

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class FastLocalParserGuardrailIntegrationTest {

    // 🟢 场景 A：高压混淆测试（验证智能分流门禁是否 100% 激活）
    @Test
    fun should_route_high_complexity_notification_with_mixed_time_domains_to_LLM() {
        val complexInput = """@所有人 请各位老师，本周六 20:00 之前完成参培统计表统计时间区间为：2025年9月1日-2026年8月31日。【金山文档】 https://www.kdocs.cn/l/cirH1TTFchqg"""

        // Act: 执行本地解析器
        val result = FastLocalParser.parse(complexInput)

        // Assert: 必须返回 null 触发路由，严禁贪婪正则截断
        assertNull("复杂混合通知应命中分流门禁，返回 null 交由 LLM 处理", result)
    }

    // 🔵 场景 B：性能保护测试（防止过度 LLM 化导致基础功能变慢）
    @Test
    fun should_keep_local_parsing_under_2ms_for_clean_simple_text() {
        val cleanInput = "明天下午 3 点开会请准时参加"

        // Warm up JIT
        FastLocalParser.parse(cleanInput)

        // Act: 计时执行
        val start = System.nanoTime()
        val result = FastLocalParser.parse(cleanInput)
        val duration = Duration.ofNanos(System.nanoTime() - start).toMillis()

        // Assert: 无链路切换，且极速响应
        assertNotNull("纯净短文本应由本地解析器直接返回", result)
        assertTrue("本地解析耗时必须严格低于 2ms (${duration}ms)", duration <= 2L)
    }

    // 🔴 场景 C：时效性契约压力测试（验证 Prompt/模型是否严格遵守截止时间优先）
    @Test
    fun llm_should_override_historical_period_in_favor_of_deadline() {
        val prompt = com.classbell.app.llm.LlmPrompts.getUniversalIngestPrompt(
            referenceDate = java.time.LocalDate.of(2026, 10, 15)
        )
        // 验证 Entity Priority Contract 注入
        assertTrue("Prompt 必须注入时间实体优先级契约", prompt.contains("时间实体优先级契约") || prompt.contains("Entity Priority Contract"))
        assertTrue("Prompt 必须确立截止时间绝对优先", prompt.contains("截止时间绝对优先") || prompt.contains("行动履约与截止时间"))
        assertTrue("Prompt 必须禁止贪婪截获历史统计区间", prompt.contains("严禁贪婪截获"))
    }

    // 🟣 场景 D：无链接的多时域混淆自动分流测试
    @Test
    fun should_route_multi_time_domain_even_without_url() {
        val multiTimeInput = "统计周期 2024-01-01 至 2025-12-31，请在下周五 17:00 前完成提交"
        val result = FastLocalParser.parse(multiTimeInput)
        assertNull("无链接但含多时间域冲突时，也应主动放行至 LLM", result)
    }
}
