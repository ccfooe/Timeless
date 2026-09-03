package com.classbell.app.llm

object LlmPrompts {

    val TIMETABLE_EXTRACTION_PROMPT = """
你是中国高校教务系统课表截图的专业解析器。
请解析图片中的课程信息，严格按照 JSON 格式输出，不要输出任何额外的解释或 Markdown 标记。

中国高校课表排版特征与解析规则：
1. 合并单元格：课程通常跨越多个节次（如 1-4 节、5-8 节）。
2. 节次与周次：类似 "(1-4节)2-17周"、"(5-8节)单周" 等。
   - periodStart / periodEnd 表示起始和结束节次（整数 1-10）。
   - weeks 表示周次范围，如 from: 2, to: 17。
   - pattern 为 "all"（每周）、"odd"（单周）、"even"（双周）。
3. 星期：根据所在列提取 dayOfWeek（1 代表周一，7 代表周日）。
4. 地点与校区：如 "金牛校区 贡院401"，campus: "金牛校区", room: "贡院401"。
5. 班级与人数：如 "25级广播电视编导03班"；人数通常标有数字或人口图标（如 49）。
6. 课程性质与考核：如 "必修"、"选修"、"考试"、"考查"。未知则设为 null。

请输出纯 JSON 数组，格式如下：
[
  {
    "name": "影视声音创作★",
    "code": "B090118",
    "dayOfWeek": 3,
    "periodStart": 1,
    "periodEnd": 4,
    "weeks": { "from": 2, "to": 17, "pattern": "all" },
    "campus": "金牛校区",
    "room": "贡院401",
    "className": "25级广播电视编导03班",
    "studentCount": 49,
    "nature": "必修",
    "examType": "考试"
  }
]
""".trimIndent()

    val CALENDAR_EXTRACTION_PROMPT = """
你是中国高校教务系统校历截图的专业解析器。
请解析图片中的学期校历信息，严格按照 JSON 格式输出，不要输出任何额外的解释或 Markdown 标记。

校历排版特征与解析规则：
1. 学期名称：如 "2026-2027学年度第一学期"。
2. 第一周周一日期：找到第1周周一对应的年月日（如 "2026-08-24"）。
3. 教学周、考试周、实践周：根据校历表格标注的周次区间提取：
   - teachingWeeks: { "from": 2, "to": 18 }
   - examWeeks: { "from": 19, "to": 20 }
   - practiceWeeks: { "from": 21, "to": 21 }
4. 节假日（休）与调休补班（补/班）：
   - 休：type 设为 "rest"，包含名称、开始日期和结束日期（如 国庆节 2026-10-01 至 2026-10-07）。
   - 补/班：type 设为 "makeup"，followsWeekday 未标明则设为 null。
5. 备注事项：提取如 "正式行课: 8月31日"、"新生军训: 9月14日-27日" 等文字。

请输出纯 JSON 对象，格式如下：
{
  "semesterName": "2026-2027学年度第一学期",
  "week1Monday": "2026-08-24",
  "teachingWeeks": { "from": 2, "to": 18 },
  "examWeeks": { "from": 19, "to": 20 },
  "practiceWeeks": { "from": 21, "to": 21 },
  "holidays": [
    { "name": "中秋节", "startDate": "2026-09-25", "endDate": "2026-09-27", "type": "rest" },
    { "name": "国庆节", "startDate": "2026-10-01", "endDate": "2026-10-07", "type": "rest" }
  ],
  "makeupWorkdays": [
    { "date": "2026-09-20", "followsWeekday": null },
    { "date": "2026-10-10", "followsWeekday": null }
  ],
  "notes": [ "正式行课: 8月31日", "新生军训: 9月14日-27日" ]
}
""".trimIndent()
}
