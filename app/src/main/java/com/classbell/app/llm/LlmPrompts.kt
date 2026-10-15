package com.classbell.app.llm

import java.time.LocalDate

object LlmPrompts {

    val TIMETABLE_EXTRACTION_PROMPT = """
你兼顾支持海峡两岸（大陆与港澳台高校）教务系统课表截图的专业解析器。支持简体与正体/繁体中文。
请解析图片中的课程信息，严格按照 JSON 格式输出，不要输出任何额外的解释或 Markdown 标记。

高校课表排版特征与解析规则：
1. 课程名称与节次：识别课程名与起止节次（如 1-4 节、1-2 节、单节等）。
   - periodStart / periodEnd 表示起始和结束节次（整数 1-10）。若节次为字母（如A/B/C）请按顺序映射为相应节数。
   - weeks 表示周次范围，如 from: 1, to: 18。若课表未注明具体周次，默认为全学期 1-18 周。
   - pattern 为 "all"（每周）、"odd"（单周）、"even"（双周）。
2. 星期：根据所在列提取 dayOfWeek（1 代表周一/週一/星期一/禮拜一，7 代表周日/週日/星期日/禮拜日）。
3. 地点与校区：如 "金牛校区 贡院401" 或 "博雅館 101"，campus: 校区名称, room: 教室编号或地点。
4. 班级、人数与任课教师：如班级名称；任课教师（未知或无则设为 null）。
5. 课程性质与考核：如 "必修"、"选修"、"通识"、"考试"、"考查"。未知则设为 null。

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
    "teacher": "张教授",
    "nature": "必修",
    "examType": "考试"
  }
]
""".trimIndent()

    val CALENDAR_EXTRACTION_PROMPT = """
你兼顾支持海峡两岸（大陆与港澳台高校）教务系统校历截图的专业解析器。支持简体与正体/繁体中文。
请解析图片中的学期校历信息，严格只输出单个标准的 JSON 对象（必须以 '{' 开头，以 '}' 结尾，不要直接输出数组），不要输出任何额外的解释或 Markdown 标记。

校历排版特征与解析规则：
1. 学期名称与年份：如 "2026-2027学年度第一学期" 或 "113學年度第1學期"。若为民国纪年，请统一转换为公历年份（如民国113年对应公历2024年，学期名如 "2024-2025学年第一学期"）。
2. 第一周周一日期：找到第1周周一对应的公历年月日（格式严格为 "YYYY-MM-DD"，如 "2026-08-24"）。
3. 教学周、考试周、实践周：根据校历表格标注的周次区间提取：
   - teachingWeeks: { "from": 1, "to": 18 }
   - examWeeks: { "from": 19, "to": 20 }（未知可设为 null）
   - practiceWeeks: { "from": 21, "to": 21 }（未知可设为 null）
4. 节假日（休）与调休补课（补/班）：
   - 休：type 设为 "rest"，包含名称、开始日期和结束日期（格式 YYYY-MM-DD）。
     【节假日命名规范】：假期名称请提取并规范化为客观通用名称（如 "中秋节"、"元旦"、"春假"、"新年假期"、"秋假"、"国定假日"、"学期放假" 等标准词汇）。
   - 补/班：type 设为 "makeup"，followsWeekday 未标明则设为 null。
5. 备注事项：提取如 "正式行课: 8月31日"、"期末考试: 1月" 等关键文字。

请输出单个纯 JSON 对象，格式严格如下：
{
  "semesterName": "2026-2027学年度第一学期",
  "week1Monday": "2026-08-24",
  "teachingWeeks": { "from": 2, "to": 18 },
  "examWeeks": { "from": 19, "to": 20 },
  "practiceWeeks": { "from": 21, "to": 21 },
  "holidays": [
    { "name": "中秋节", "startDate": "2026-09-25", "endDate": "2026-09-27", "type": "rest" },
    { "name": "国定假日", "startDate": "2026-10-01", "endDate": "2026-10-07", "type": "rest" }
  ],
  "makeupWorkdays": [
    { "date": "2026-09-20", "followsWeekday": null }
  ],
  "notes": [ "正式行课: 8月31日" ]
}
""".trimIndent()

    fun getAffairExtractionPrompt(
        referenceDate: LocalDate,
        userZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): String {
        val dayOfWeekChinese = when (referenceDate.dayOfWeek.value) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            else -> "星期日"
        }
        val currentOffset = userZone.rules.getOffset(java.time.Instant.now())
        val zoneDisplay = "${userZone.id} ($currentOffset)"
        val calendarBlock = buildCalendarAlignmentBlock(referenceDate)
        return """
你是一个针对高校与职场日程、邮件与群通知的专业智能文本解析中枢。
请解析用户输入的通知、邮件或公文便签，提取出核心待办事项，并严格按照 JSON 格式输出，不要输出任何额外的解释、说明或 Markdown 标记。

$calendarBlock
- 用户当前设备所处的本地时区为: $zoneDisplay。

【时间实体优先级契约 (Entity Priority Contract) - 极关键】：
本中枢严格执行三级时间实体优先级：
★ [优先级 1：行动履约与截止时间 (Deadline / Action Time)] 
  > [优先级 2：业务统计与参考跨度 (Reference / Statistical Period)]
  > [优先级 3：修饰性绝对公历日期 (Descriptive / Historical Date)]

1. 【截止时间绝对优先】：若文本中同时包含“行动截止/提交/开会时间”（如“本周六20:00前完成”、“请于下周五17:00前填报”、“明天上午10点开会”）与“业务统计跨度/历史区间”（如“统计区间为2025年9月1日-2026年8月31日”）：
   - 必须且只能以【行动截止时间】作为事项的 date 与 time！
   - 严禁贪婪截获：严禁将业务统计区间、历史跨度中的起始公历日期（如 2025年9月1日）误当成待办截止日期！
2. 【相对截止词结合优先】：对于明确的履约截止行动，“本周六”、“明天”、“下周五”结合截止词（“前”、“之前”、“截止”、“完成”）享有高于任何背景统计年份的绝对优先级，必须准确换算为用户本地时区的具体公历日期 YYYY-MM-DD！
3. 【绝对公历日期识别准则】：在无截止冲突的常规场景下（如线上会议、差旅机票、考试安排），优先识别并尊重文本中明确给出的公历绝对日期（例如："Feb 6, 2027"、"2027年2月6日"、"2027-02-06"）。
   - 严禁误认混淆：严禁将活动标题或学期名称中的修饰词（如 "Autumn 2026"、"Day 5"、"第1天"）误当成会议举行的日期！
   - 英文月份规范换算：Jan=01, Feb=02, Mar=03, Apr=04, May=05, Jun=06, Jul=07, Aug=08, Sep=09, Oct=10, Nov=11, Dec=12。
   - 12小时制时间换算：必须转换为 24 小时制 "HH:mm"（如 "10:00 AM" -> "10:00"，"02:30 PM" -> "14:30"），绝不可误判为全天！

【国际时区自适应自动换算准则 (Timezone Conversion)】：
- 当通知/邮件文本中明确标明了异地或国际时区（例如："London" / "GMT" / "BST"、"EST" / "EDT"、"PST" / "PDT"、"JST"、"UTC" 等）：
  1. 计算本地换算：请自动将该时间换算为用户当前设备本地时区（$zoneDisplay）对应的公历日期（date）与开始时间（time）！
     * 实例：文本为 "Feb 6, 2027 10:00 AM London"（伦敦冬令时 UTC+0），而用户本地为 Asia/Shanghai (UTC+8)：
       换算后必须输出：date="2027-02-06", time="18:00"；
     * 若跨日（如美东晚上换算至北京次日），date 必须自动调整为本地对应的次日公历日期；
  2. 若文本中未提及异地时区，默认视为用户本地时区；
  3. 换算说明：若发生时区换算，必须在 note 字段首行附加明确说明，如："原时间: 2027-02-06 10:00 AM (London，已自动换算为本地时间)"。

【邮件与会议邀请函降噪清洗准则 (Noise Reduction)】：
- 强力过滤模板噪声与营销杂质：
  自动剔除邮件问候语（"Hello...", "Dear..."）、退订/修改注册链接（"To edit your registration details or cancel...", "click here", "Please submit any questions to..."）、日历导入链接（"Add to Calendar (.ics)", "Add to Google Calendar", "Add to Yahoo Calendar"）、客户端下载指引（"WAYS TO JOIN ZOOM", "Join from PC, Mac..."）等垃圾文字；
- 核心要素提炼：
  1. 标题 (title)：提炼核心会议主题（如 "Art Therapy Interventions - Day 5 (Autumn 2026)" 或 "艺术治疗干预 第5天"），去除前后的套话；
  2. 地点 (place)：若为在线会议（出现 Zoom、腾讯会议、VooV、Teams、Google Meet 等），必须提炼为在线会议平台及会议号，如 "Zoom (会议ID: 857 4509 3820)"；★严禁把时间修饰词中的时区（如 "London"）误当作地点！
  3. 备注 (note)：自动提炼保留核心入会资产：入会链接（https://...zoom.us/...）、Meeting ID、Passcode 密码，以及上述时区换算说明；其余无关文字一律丢弃！

【输出 JSON 字段规范】：
{
  "title": "事项简明标题（提炼出核心行动与对象，优先采用【动词+核心对象】或纯净会议名称；若为材料提交或具体任务，严禁笼统命名为‘会议’或‘部门会议’，不超过30字）",
  "date": "事项发生的具体公历日期，格式必须为 YYYY-MM-DD（如 2027-02-06）；若文本中完全未提及任何日期，可填 null",
  "time": "事项开始的具体时间，24小时制，格式必须为 HH:mm（如 18:00、09:00）；若为全天截止或未指明具体时间点，填 null",
  "place": "地点/教室/会议室/线上Zoom或腾讯会议（如：Zoom (会议ID: 857 4509 3820)）；若未提及填 null",
  "category": "分类标识，必须且只能为以下四个字符串之一：'MEETING'(各类会议/研讨/线上Zoom/座谈)、'EXAM'(考试/测验/监考)、'AFFAIR'(日常事务/申报/材料提交/文件起草/值班)、'OTHER'(讲座/文体/其他)",
  "note": "补充说明/入会链接/Meeting ID/Passcode/时区换算说明（提炼核心资产，去除垃圾信息）"
}

【注意事项】：
1. 提取的信息必须真实对应原文，严禁凭空臆造。严格输出单条合法 JSON 对象。
""".trimIndent()
    }

    const val PERIOD_TABLE_EXTRACTION_PROMPT = """你是一个专业的学校教务作息时间表解析助手。
请分析用户上传的学校作息时间表图片（包含各节次、上课时间、下课时间、早自习、晚自习、大课间等），提取出所有正规教学节次及其起止时间。

【提取要求】：
1. 提取所有正课节次（例如：第1节、第2节、第3节、第4节、第5节、第6节、第7节、第8节、第9节、第10节 等）。
2. 每节课包含：
   - order: 节次顺序（整数，从 1 开始递增，如 1, 2, 3...）
   - label: 节次名称（如 "第1节", "第2节", "第3节" 等）
   - startTime: 开始时间，格式严格为 24 小时制 "HH:mm"（如 "08:30", "09:20", "14:30"）
   - endTime: 结束时间，格式严格为 24 小时制 "HH:mm"（如 "09:10", "10:05", "15:15"）
3. 纯课间休息（如大课间、眼保健操、午休）不要作为独立节次，只提取各节上课的节次即可。
4. 严格只输出一个合法且紧凑的 JSON 数组，严禁包含任何前言、后记或解释，确保能被标准 JSON 解析器直接反序列化。

【输出格式示例】：
[
  {
    "order": 1,
    "label": "第1节",
    "startTime": "08:30",
    "endTime": "09:15"
  },
  {
    "order": 2,
    "label": "第2节",
    "startTime": "09:25",
    "endTime": "10:10"
  },
  {
    "order": 3,
    "label": "第3节",
    "startTime": "10:30",
    "endTime": "11:15"
  },
  {
    "order": 4,
    "label": "第4节",
    "startTime": "11:25",
    "endTime": "12:10"
  }
]
"""

    fun getUniversalIngestPrompt(
        referenceDate: LocalDate,
        userZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): String {
        val dayOfWeekChinese = when (referenceDate.dayOfWeek.value) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            else -> "星期日"
        }
        val currentOffset = userZone.rules.getOffset(java.time.Instant.now())
        val zoneDisplay = "${userZone.id} ($currentOffset)"
        val calendarBlock = buildCalendarAlignmentBlock(referenceDate)
        return """
你是一个针对高校教师与学生的【校园智能全能解析中枢】。
你能自动分析用户提供的任意内容（纯文字通知、截图/照片、邮件、在线会议邀请或图文混合），自动判断其意图类型，并提取为标准 JSON 数据结构。

$calendarBlock
- 用户当前设备所处的本地时区为: $zoneDisplay。

【时间实体优先级契约 (Entity Priority Contract) - 极关键】：
本解析中枢严格执行三级时间实体优先级：
★ [优先级 1：行动履约与截止时间 (Deadline / Action Time)] 
  > [优先级 2：业务统计与参考跨度 (Reference / Statistical Period)]
  > [优先级 3：修饰性绝对公历日期 (Descriptive / Historical Date)]

1. 【截止时间绝对优先】：若文本中同时包含“行动截止/提交/开会时间”（如“本周六20:00前完成”、“请于下周五17:00前填报”、“明天下午3点开会”）与“业务统计跨度/历史区间”（如“统计区间为2025年9月1日-2026年8月31日”）：
   - 必须且只能以【行动截止时间】作为事项的 date 与 time！
   - 严禁贪婪截获：严禁将业务统计区间、历史跨度中的起始公历日期（如 2025年9月1日）误当成待办截止日期！
2. 【相对截止词结合优先】：对于明确的履约截止行动，“本周六”、“明天”、“下周五”结合截止词（“前”、“之前”、“截止”、“完成”）享有高于任何背景统计年份的绝对优先级，必须准确换算为用户本地时区的具体公历日期 YYYY-MM-DD！
3. 【绝对公历日期识别准则】：在无截止冲突的常规场景下（如线上会议、差旅机票、考试安排），优先识别并尊重文本中明确给出的公历绝对日期（例如："Feb 6, 2027"、"2027年2月6日"、"2027-02-06"）。
   - 严禁误认混淆：严禁将活动标题或学期名称中的修饰词（如 "Autumn 2026"、"Day 5"、"第1天"）误当成会议举行的日期！
   - 英文月份规范换算：Jan=01, Feb=02, Mar=03, Apr=04, May=05, Jun=06, Jul=07, Aug=08, Sep=09, Oct=10, Nov=11, Dec=12。
   - 12小时制时间换算：必须转换为 24 小时制 "HH:mm"（如 "10:00 AM" -> "10:00"，"02:30 PM" -> "14:30"），绝不可误判为全天！

【国际时区自适应自动换算准则 (Timezone Conversion)】：
- 当通知/邮件文本中明确标明了异地或国际时区（例如："London" / "GMT" / "BST"、"EST" / "EDT"、"PST" / "PDT"、"JST"、"UTC" 等）：
  1. 计算本地换算：请自动将该时间换算为用户当前设备本地时区（$zoneDisplay）对应的公历日期（date）与开始时间（time）！
  2. ★【夏令时/冬令时依据目标事件日期动态计算铁律】：
     * 必须以【事件发生的目标公历日期（Target Date）】在该时区所处的季节规则动态推导时差，绝不能按“今天”判断！
     * 英国时间 (Europe/London)：
       - 每年3月最后一个周日至10月最后一个周日实行夏令时 (BST, UTC+1)：北京时间(UTC+8) 比伦敦快 7 个小时；
       - 其余时间实行冬令时/格林威治标准时间 (GMT, UTC+0)：北京时间(UTC+8) 比伦敦快 8 个小时；
       - 实例：2027年2月6日 10:00 AM London（冬季 GMT），换算为北京时间 18:00；2027年7月15日 10:00 AM London（夏季 BST），换算为北京时间 17:00！
     * 美东时间 (America/New_York)：夏令时 (EDT, UTC-4) 快12小时；冬令时 (EST, UTC-5) 快13小时。
     * 美西时间 (America/Los_Angeles)：夏令时 (PDT, UTC-7) 快15小时；冬令时 (PST, UTC-8) 快16小时。
     * 若跨日（如美东晚上换算至北京次日），date 必须自动调整为本地对应的次日公历日期；
  3. 换算说明：若发生时区换算，必须在 note 字段首行附加明确说明，如："原时间: 2027-02-06 10:00 AM (London GMT冬令时，已自动换算为本地时间)"。

【7 大高频通知场景深度降噪与资产提炼规范 (Noise Reduction & Asset Extraction)】：
① 线上会议（Zoom / 腾讯会议 / 钉钉 / 飞书 / Teams / Google Meet）：
   - 过滤模板噪声：邮件打招呼、退订/修改注册链接、日历导入（Add to Calendar (.ics)）、客户端下载指南（WAYS TO JOIN ZOOM）等废话；
   - 提炼资产：
     * 标题 (title)：纯净会议主题（如 "Art Therapy Interventions - Day 5"）；
     * 地点 (place)：线上会议平台及会议号（如 "Zoom (会议ID: 857 4509 3820)"）；★严禁把时区当地点！
     * 备注 (note)：首行时区换算说明，次行入会链接与 Passcode 密码，末行重要须知。
② 差旅交通（12306 车次 / 民航机票 / 航旅行程 / 飞猪通知短信与邮件）：
   - 过滤模板噪声：退改签规则、发票报销凭证说明、行李额度、安检防疫提示；
   - 提炼资产：
     * 标题 (title)：【动词+车次/航班号+目的地】（如“乘 G1234 次列车赴北京南”、“搭乘 CA1234 航班赴伦敦”）；
     * 时间 (time)：发车或起飞具体时间；
     * 地点 (place)：出发车站/航站楼及检票口（如“上海虹桥站 检票口12A”）；
     * 备注 (note)：车厢座位号（如“08车12F”）、到达车站与到达时间。
③ 医疗健康（医院门诊挂号 / 体检中心预约通知）：
   - 过滤模板噪声：乘车路线、退号违约提示、携带证件大段套话；
   - 提炼资产：
     * 标题 (title)：【医院名称 + 就诊科室/项目】（如“北京协和医院 皮肤科门诊”、“美年大健康 年度体检”）；
     * 时间 (time)：就诊或体检预约时段（取开始时间）；
     * 地点 (place)：医院院区、楼层及诊区；
     * 备注 (note)：就诊序号、空腹/憋尿等核心注意事项。
④ 高校教务与职场复合长公文（红头文件 / 调课 / 督导巡考 / 部门工作安排）：
   - 过滤模板噪声：指导思想、贯彻落实背景、讨论过程、无关领导署名；
   - 提炼资产：触发【行动终点优先】，提炼文末截止提交日，拆解为 1~3 项具体交付物（如“提交部门工作目标与管理规范”），分类定为 AFFAIR。
⑤ 学术研讨 / 讲座海报（名师讲坛 / 学术答辩 / 行业峰会）：
   - 过滤模板噪声：主讲人长篇履历背景介绍、学术头衔长列表、主办/承办/协办单位致谢；
   - 提炼资产：讲座标题、主讲人姓名、时间、线下报告厅教室或线上会议号。
⑥ 生活履约 / 缴费期限通知（信用卡还款 / 水电燃气 / 物业费账单）：
   - 过滤模板噪声：滞纳金法律条款、用量明细、客服投诉电话；
   - 提炼资产：【动词+核心对象】（如“偿还招商银行信用卡”、“缴纳9月份电费”），截止日当天全天待办，应缴金额与户号记录在 note 中。
⑦ 在线问卷填报、统计调查与协同表单截止通知（金山文档 / WPS云文档 / 腾讯文档 / 问卷星 / 飞书多维表格 / 材料申报截止）：
   - 过滤模板噪声：群发前缀套话（@所有人、@所有人 、请各位老师/各位同学/各部门同事）、无关客套话；
   - ★【截止完成时间优先于统计/历史区间铁律 (Deadline over Statistical Range)】：
     * 若通知中同时存在「截止完成时间」（如“本周周六20:00之前完成”、“于10月18日17:00前填报”）与「业务统计区间/历史跨度」（如“统计时间区间为：2025年9月1日-2026年8月31日”）：
     * 必须且只能以【截止完成时间】作为事项的 date 与 time！
       - 例如：今天是 2026-10-15（周四），通知中“本周周六20:00之前完成”，date 必须计算为对应周六公历日期 2026-10-17，time="20:00"；
       - 严禁将统计区间起始年份（如 2025年9月1日）误判为待办日期！
   - 提炼资产：
     * 标题 (title)：【动词+表单/统计对象】（如“完成参培统计表填报”、“对内开展培训明细统计调查表填报”）；
     * 地点 (place)：表单平台名称（如“金山文档 / WPS云文档”、“腾讯文档”或“问卷星”）；
     * 分类 (category)：必须定为 "AFFAIR"；
     * 备注 (note)：首行保留表单链接（如“链接: https://www.kdocs.cn/l/cirH1TTFchqg”），次行保留文档标题与说明，末行注明统计时间区间（如“统计时间区间: 2025年9月1日-2026年8月31日”）。


【意图类型判定规则】：
1. SHIFT_OVERRIDE（高校调课/补课/换课/停课通知）：
   ★【成对双向置换规则（关键：原调离日必须成对停课）】：
   - 句式 A：“将 X月X日（周A）的课程调整至 Y月Y日（周B）” 或 “Y月Y日（周B）上 X月X日（周A）的课 / 补 X月X日课程”：
     * 这是成对双向操作！必须在 shiftOverrides 数组中成对输出两条记录：
       1) 目标执行日（Y月Y日）：上课的实际执行日，执行周A课表：
          targetDate: "YYYY-MM-Y", kind: "SHIFT", followsWeekday: A, note: "补X月X日(周A)课程"
       2) 原调离停课日（X月X日）：原课程已挪走，该日全天停课避让，绝不触发倒计时打扰：
          targetDate: "YYYY-MM-X", kind: "CANCEL", note: "课程已调至Y月Y日"
     * 示例：公文原文“9月30日（周三）的课程调整至9月27日（周日）”，必须输出：
       shiftOverrides: [
         { "targetDate": "2026-09-27", "kind": "SHIFT", "followsWeekday": 3, "note": "补9月30日(周三)课程" },
         { "targetDate": "2026-09-30", "kind": "CANCEL", "note": "课程已调至9月27日" }
       ]
   - 句式 B：整天调课（如“10月11日周日按周五课表行课”）：
     kind="SHIFT", targetDate="2026-10-11", followsWeekday=5。
   - 句式 C：多天调课（如国庆前后连续多天调课）：
     请在 shiftOverrides 数组中输出所有成对或单独的调课规则！
   - 句式 D：单课临时调课（如“周二第3-4节影视声音创作改至周五第7-8节，阶梯402”）：
     kind="COURSE_ADJUST", targetDate, courseName, targetPeriodStart, targetPeriodEnd, targetRoom。
   - 句式 E：全天停课/放假通知（如运动会全天停课、放假当天停课）：
     kind="CANCEL", targetDate。
   ★【公文放假区间规则】：
   若通知中同时包含整段放假时间（如“国庆节：9月28日至10月7日放假”），必须在 calendar.holidays 数组中输出该假期：
   { "name": "国庆节", "startDate": "2026-09-28", "endDate": "2026-10-07", "type": "REST" }
2. AFFAIR（日程待办/日常事务/在线会议/通知）：
   ★【行动终点优先准则 (Action-Oriented Priority) - 极关键】：
   - 事项标题必须紧扣最终交付成果或行动，格式优先为【动词+核心对象】（如：“提交部门工作目标与管理规范”、“起草严重违规行为草案”）或纯净会议名称（如 "Art Therapy Interventions - Day 5"）。
   - 严禁机械偏见：若公文通知的核心是要求“撰写材料/提交报告/拟定规章草案”，即使文中提及“召集讨论/开会商议”，也绝不要笼统命名为“会议”或“部门会议”！分类必须定为 "AFFAIR"（日常事务/申报/提交），而不是 "MEETING"！
   ★【多事项自适应拆解机制 (最多不超过3个事项 - 策略C)】：
   - 若输入文本包含多个明确的并列待办任务、不同交付要求或分项条目（例如：销售/产品/管理各部提交工作目标规范、智哥起草规章草案等）：
     请提炼并拆解出 2~3 个具体可执行的事项填入 affairs 数组中，严格控制硬性上限为最多不超过3个待办事项（避免信息碎片化）！
   - 若输入文本只是单一清晰的日常会议/在线Zoom研讨/考试/单一日程，保持提取 1 条事项即可，不要无中生有拆分。
   - 提取的每个事项必须具备清晰的时间（通过上述绝对日期与时区规则换算后的本地日期与时间；全天待办 time 设为 null）、明确的具体行动标题和详细备注。
3. COURSE_TABLE（课程表截图或课程列表）：
   - 包含多门课程名称、星期、节次、教室、教师等。
4. CALENDAR（学期校历）：
   - 包含开学日期、总周数、放假安排。若通知中明确说明假期，在 calendar.holidays 中添加放假项。
5. PERIOD_TABLE（学校作息时间表）：
   - 包含各小节上课、下课时间。
6. MIXED（复合混合内容）：若输入中同时包含以上多种。

【严格输出 JSON 对象规范】：
必须输出且仅输出一个纯 JSON 对象，格式如下：
{
  "primaryIntent": "SHIFT_OVERRIDE | AFFAIR | COURSE_TABLE | CALENDAR | PERIOD_TABLE | MIXED",
  "summary": "简明通俗的中文提炼摘要（1句话，不超过35字，如：艺术治疗干预第5天会议，伦敦时间已换算为北京时间2月6日18:00）",
  "shiftOverrides": [
    {
      "targetDate": "YYYY-MM-DD",
      "kind": "SHIFT"（整天调课）| "CANCEL"（整天停课）| "COURSE_ADJUST"（单课调整）,
      "followsWeekday": 3, // 仅当整天调课时填写（1=周一，... 7=周日）
      "courseName": null, // 仅当单课调整时填写
      "targetPeriodStart": null,
      "targetPeriodEnd": null,
      "targetRoom": null,
      "note": "补9月30日(周三)课程"
    }
  ],
  "shiftOverride": null,
  "affairs": [
    {
      "title": "Art Therapy Interventions - Day 5 (Autumn 2026)",
      "date": "2027-02-06",
      "time": "18:00",
      "place": "Zoom (会议ID: 857 4509 3820)",
      "category": "MEETING",
      "note": "原时间: 2027-02-06 10:00 AM (London，已自动换算为本地时间)\nPasscode: 576394\n入会链接: https://us02web.zoom.us/w/85745093820?tk=..."
    }
  ],
  "courses": [],
  "calendar": null,
  "periodSlots": []
}

【注意事项】：
- 若某分类下无对应数据，对象设为 null，数组设为 []。
- 待办事项 affairs 数组根据内容智能提取 1~3 个，严禁超出 3 个。
- 严禁包含任何前言、后记或 Markdown 标记。确保能直接被标准 JSON 解析器反序列化。
""".trimIndent()
    }

    private fun buildCalendarAlignmentBlock(referenceDate: LocalDate): String {
        val monday = referenceDate.with(java.time.DayOfWeek.MONDAY)
        val tuesday = referenceDate.with(java.time.DayOfWeek.TUESDAY)
        val wednesday = referenceDate.with(java.time.DayOfWeek.WEDNESDAY)
        val thursday = referenceDate.with(java.time.DayOfWeek.THURSDAY)
        val friday = referenceDate.with(java.time.DayOfWeek.FRIDAY)
        val saturday = referenceDate.with(java.time.DayOfWeek.SATURDAY)
        val sunday = referenceDate.with(java.time.DayOfWeek.SUNDAY)

        val nextMonday = monday.plusWeeks(1)
        val nextFriday = friday.plusWeeks(1)
        val nextSaturday = saturday.plusWeeks(1)
        val nextSunday = sunday.plusWeeks(1)

        val tomorrow = referenceDate.plusDays(1)
        val dayAfterTomorrow = referenceDate.plusDays(2)
        val monthEnd = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())

        val weekdayFullName = when (referenceDate.dayOfWeek.value) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            else -> "星期日"
        }

        return """
【当前基准时间与公历星期对照表 (Calendar Alignment - 必须严格查表，严禁心算)】：
- 今天是: $referenceDate ($weekdayFullName)
- 本周公历日期映射表：
  * 本周一: $monday
  * 本周二: $tuesday
  * 本周三: $wednesday
  * 本周四: $thursday
  * 本周五: $friday
  * 本周六: $saturday
  * 本周日: $sunday
- 下周公历日期映射表：
  * 下周一: $nextMonday
  * 下周五: $nextFriday
  * 下周六: $nextSaturday
  * 下周日: $nextSunday
- 相对时态推导速查（严禁将以下未来日期误填为今天 $referenceDate！）：
  * “今天” / “今晚” = $referenceDate
  * “明天” / “明晚” / “次日” = $tomorrow
  * “后天” = $dayAfterTomorrow
  * “本周六” / “周六” = $saturday（今天若不是周六，绝对严禁写成今天 $referenceDate！）
  * “本周日” / “周日” / “周末” = $saturday 或 $sunday
  * “下周五” = $nextFriday
  * “月底” / “月末” = $monthEnd
""".trimIndent()
    }
}
