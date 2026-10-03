# ClassBell 领域通用词汇表 (GLOSSARY.md)

> 本文档基于领域驱动设计（DDD）思想建立，作为人类开发者与 AI Agent 之间的 **Ubiquitous Language（通用语言）**。
> 无论是在需求沟通、架构设计、代码命名（类名/变量名/方法名）还是日志提示中，所有概念均需严格遵循本文档定义的术语。

---

## 1. 基础日历与学期模型 (Calendar & Semester)

| 术语 (Term) | 对应代码类型 / 符号 | 定义与业务含义 |
| :--- | :--- | :--- |
| **学期** (Semester) | [`Semester`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/data/model/Semester.kt) | 教学学期实体，包含学期名称、起止日期（`startDate`, `endDate`）、总周数（`totalWeeks`）以及考试周/实训周范围。 |
| **教学周** (Academic Week) | `weekNumber: Int` (1..25) | 相对学期开始日期的第几周（基于星期一为每周起始点）。 |
| **周几 / 星期** (Weekday) | `weekday: Int` (1=Mon..7=Sun) | 星期数字，遵循 ISO-8601 标准（周一为 1，周日为 7）。 |
| **法定/全校假期** (Holiday) | [`Holiday`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/data/model/Semester.kt) | 属于整段连续放假停课的区间（如国庆假 9.28~10.7），存储在 `Semester.holidays` 中。 |

---

## 2. 课程与小节结构 (Course & SubPeriod)

一个物理课程（`Course`）通常跨越若干个连堂小节（如上午 1~4 节，或 3~4 节）。

| 术语 (Term) | 对应代码类型 / 字段 | 定义与业务含义 | 禁忌 / 易错点 |
| :--- | :--- | :--- | :--- |
| **大节 / 课程** | [`Course`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/data/model/Course.kt) | 数据库中的排课记录。包含 `startPeriod` 和 `endPeriod`（如 1 到 4）。 | 不要与单小节混淆。 |
| **小节作息槽位** | `PeriodSlot` | 学校作息时间表中定义的小节，包含节次序号（1~12）、开始时间与结束时间。 | - |
| **当前小节序号** | `periodOrder: Int` | 当前小节在全天作息中的绝对序号（如第 1 节、第 5 节）。 | **严禁写成 `slotOrder`**。 |
| **小节内序号** | `periodIndexInCourse: Int` | 当前小节在本门连堂课中的相对位置（如 4 节连堂中的第 1 节）。 | 1-indexed。 |
| **总小节数** | `totalPeriodsInCourse: Int` | 该大节包含的小节总数（例如 4 节连堂则为 4）。 | - |
| **小节显示标签** | `periodLabel: String` | 面向用户的格式化文案，例如 `"第1节"`、`"第5节"`。 | **严禁写成 `label`**。 |
| **小节动态状态** | [`SubPeriodState`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/data/model/SubPeriodState.kt) | 包含当前实时小节类型（上课/课间/课前准备/完课）、剩余秒数、真实进度百分比。 | 随每秒 tick 动态计算。 |
| **课间休息** | `SubPeriodKind.BREAK` | 连堂大课中两个小节之间的时间窗口（如 10 分钟课间、25 分钟大课间）。 | 课间倒计时独立渲染。 |

---

## 3. 调课与停课机制 (Override System)

高校公文调整课表时的核心模型，由 [`Override`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/data/model/Override.kt) 承载。

| 调课行为 | 对应 `OverrideType` | 字段与业务含义 | 表现效果 |
| :--- | :--- | :--- | :--- |
| **调课执行 (Shift)** | `OverrideType.IS_SHIFT` | **执行补课的日期**。包含 `targetDate` 与 `followsWeekday`。<br>例如：“9月27日上9月30日（周三）的课”，则 9.27 为 SHIFT，`followsWeekday = 3`。 | 日历和首页标【调】，运行周三课程与倒计时。 |
| **调离停课 (Cancel)** | `OverrideType.IS_CANCEL` | **原课程被借走或停课的日期**。<br>例如：“9月30日课程已调整至9月27日”，则 9.30 为 CANCEL，附带 `note` 备注。 | **零打扰**：阻断倒计时，日历/首页画删除线并打【已调出】徽章。 |
| **临时补课 (Manual)** | `OverrideType.MANUAL_SESSION` | 教师临时发起的随堂点名或单次加课。 | 拥有独立的计时窗口。 |

---

## 4. 会话级免打扰与提前下课 (Session Suppression)

| 术语 (Term) | 对应代码 / 概念 | 定义与业务含义 | 持久化特性 |
| :--- | :--- | :--- | :--- |
| **会话抑制** | `SessionSuppression` | 用户在运行态主动发起的免打扰限制模型（包含 `suppressedPeriodSlots`, `suppressMorning`, `suppressFullDay`）。 | **纯内存级**（重启或跨天自动重置），**绝不修改数据库排课表**。 |
| **结束本节课** | `dismissCurrentPeriod()` | 用户提前下课。仅结束当前第 N 小节，课间结束后下一小节继续提醒。 | 停止前台服务并记录小节免打扰。 |
| **结束本半天** | `dismissCurrentHalfDay()` | 下午或上午后续课程不再提醒，直到中午 13:00 或次日。 | 停止服务，半天内全部静音。 |
| **结束全天** | `dismissCurrentFullDay()` | 今日全天不再拉起任何上课通知与倒计时。 | 停止服务，全天静音。 |

---

## 5. 排程与运行引擎 (Engine & Services)

| 术语 (Term) | 对应组件 / 文件 | 职责与生命周期 |
| :--- | :--- | :--- |
| **排程引擎** | [`OccurrenceEngine`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/core/OccurrenceEngine.kt) | 纯 Kotlin 领域模块。负责结合基础课表、学期周数、调课规则（`Override`）、放假状态（`Holiday`）与实时时间，输出今日所有排程实例（`MeetingOccurrence`）。 |
| **上课前台服务** | [`InClassService`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/service/InClassService.kt) | Android Foreground Service。在上课期间常驻，维系系统锁屏通知卡片与第二级高精度倒计时。 |
| **精准闹钟调度器** | [`AlarmScheduler`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/service/AlarmScheduler.kt) | 基于 Android `AlarmManager` 唤醒课前预备、上下课打铃广播。 |
| **公文快析器** | [`FastLocalParser`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/core/FastLocalParser.kt) | 端侧离线极速正则解析器，毫秒级提取常见公文中的成对调课与日程。 |
| **云端多模态解析** | [`LlmClient`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/llm/LlmClient.kt) | 智谱 GLM-4v-plus 多模态大模型客户端，支持公文截图、复杂自然语言智能提取。 |
| **系统日历同步** | [`CalendarSyncManager`](file:///Users/lichao/Project_ClassBell/app/src/main/java/com/classbell/app/core/CalendarSyncManager.kt) | 双向或单向将排程写入手机系统日历 ContentProvider。 |
