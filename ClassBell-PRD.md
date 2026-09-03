# ClassBell — University Teacher Companion App
## Product Requirements Document / Development Specification (v1.0)

> **Purpose of this document**: This is a complete, self-contained development brief intended to be handed directly to a coding agent (Anti-Gravity or similar) to build the app from scratch.
> **Date**: 2026-09-03 · **Status**: Requirements clarified with the product owner (the teacher). No further input required to start M0–M5, except one pending item flagged in §14.

---

## 1. Overview

### 1.1 Background
The product owner is a university teacher at **Sichuan Film and Television University (四川电影电视学院)**, teaching on a campus that uses the **ColorOS 16** system (OnePlus 12). Their pain points:

1. The school bell system is unreliable and sometimes fails, so the teacher **overruns class time (拖堂)** without realizing it.
2. The school provides an **academic calendar (校历)** and a **course timetable (课程表)** from the academic affairs system, but there is no good mobile way to see "what week is it, what do I teach today, to which class, how many students."
3. School events (meetings, exams, deadlines) have no unified reminder place.

### 1.2 Product Goal
A **native Android app** that:
- Imports the semester calendar and personal course timetable **from screenshots via a multimodal LLM API** (with manual editing as fallback).
- Always shows the **current week number**, date, courses, **teaching class (授课班级)** and **student count (人数)**.
- During class, shows a **frosted-glass, iOS-quality progress card** — both **in-app** and as a **persistent lock-screen notification styled like the NetEase Cloud Music player** — so the teacher always knows the **class-end time** and how much time remains.
- Provides a **school-affairs calendar with reminders** that survive background killing on ColorOS.
- Ships as a single **shareable APK**; each teacher installs it and manages their own data locally.

### 1.3 Provisional Name
**ClassBell (working title)** — package id `com.classbell.app`. The owner may rename before release; keep the name a single string resource.

---

## 2. Glossary

| Term | Meaning |
|------|---------|
| 校历 / Academic Calendar | School-published semester calendar: week numbering, teaching/exam/practice weeks, holidays, make-up workdays |
| 周次 / Week number | Semester week index. Week 1 = the week (Mon–Sun) containing the semester start date. E.g. 2026-08-24 (Mon) starts week 1 of 2026–2027 Fall semester |
| 节次 / Period | A teaching slot (1–8 per day in the reference school: periods 1–4 morning, 5–8 afternoon). Each period maps to exact clock times via a configurable period-time table |
| 教学周 / Teaching weeks | Weeks when classes run (reference semester: weeks 2–18) |
| 授课班级 / Teaching class | The student cohort, e.g. 25级广播电视编导03班 |
| 调休 / 补 (Make-up workday) | A rest day swapped into a workday (marked 补 on the calendar); classes may follow a swapped day-of-week schedule |
| 单双周 / Odd-even weeks | Courses that run only on odd or even weeks (must be supported even if the owner's current courses run continuously) |

---

## 3. Users, Devices, Distribution

| Item | Decision |
|------|----------|
| Users | University teachers (owner first; shared to colleagues via APK) |
| **Multi-school by design** | The app is **school-agnostic**: nothing about the reference school is hard-coded. Every teacher configures their own **semester calendar (imported via LLM)**, **period-time table** (FR-9.1), and **courses** (imported via LLM or manual). Appendix A–C data is only first-launch seed for the owner |
| Target device | OnePlus 12, ColorOS 16 (Android 16), 6.7" |
| minSdk / targetSdk | minSdk 29 (Android 10) / targetSdk 36 (Android 16) |
| Accounts / Server | **None.** Fully offline-first, local database only |
| Distribution | Self-signed **release APK** shared via Android share sheet (WeChat / file transfer). Each teacher enters their own data |
| Language | zh-CN UI (primary). All strings in `strings.xml` to allow future locales |

---

## 4. Scope

### 4.1 In Scope (v1.0)
1. Semester management + academic-calendar-driven week number display.
2. Course timetable (weekly recurring courses with week ranges/patterns, periods, rooms, classes, student counts).
3. **LLM-powered import** of timetable screenshots and calendar screenshots (BYOK), with a confirmation/editing step.
4. Now-in-class **frosted-glass progress card** in-app + **persistent lock-screen notification** (media-style, progress bar, end time) + **5-minutes-before-end reminder**.
5. School-affairs calendar with reminders (exact alarms).
6. Teaching stats (classes taught, accumulated 学时) — **no income/hourly-rate features**.
7. Local data with **export/import backup file**.

### 4.2 Out of Scope (v1.0)
- Cloud sync, accounts, server of any kind.
- iOS / desktop.
- Excel import (revisit in v1.1 if requested — LLM import covers it).
- Home-screen AppWidget (roadmap P2, §13).
- Student-facing features, gradebook, attendance.

---

## 5. Confirmed Product Decisions

These were explicitly confirmed with the owner via structured Q&A — do not reopen them:

| # | Decision |
|---|----------|
| D1 | **Tech stack: 100% native Kotlin + Jetpack Compose** (previous Capacitor/web prototype is abandoned). Reasons: reliable foreground services, exact alarms, lock-screen notification, no WebView overhead |
| D2 | Timetable/calendar data enters the app via **multimodal LLM API** (screenshot → structured JSON), with a mandatory **confirmation page** before saving; manual create/edit always available |
| D3 | LLM access is **BYOK (Bring Your Own Key)**: OpenAI-compatible base URL + API key + model name entered in Settings. No key is shipped inside the APK |
| D4 | Lock-screen display = **persistent foreground-service notification**, NetEase-Cloud-Music style (course, class, room, end time, progress bar). No full-screen intent alarms |
| D5 | Class-end reminder: **5 minutes before class end** (configurable). **No before-class reminder** (owner declined) |
| D6 | The calendar screenshot import also drives semester config (week 1 start date, week ranges, holidays, make-up days) |
| D7 | Stats: **keep teaching-hour statistics, remove all income/hourly-rate features** |
| D8 | Data: local-only + **export/import backup file** (single JSON, shareable via share sheet) |
| D9 | The owner's current semester has exactly **two courses**, both Wednesday weeks 2–17 (seed data in Appendix B) |
| D10 | Owner supplied the school's **period-time table (作息时间表)** on 2026-09-03 — Appendix C is now **authoritative seed data** (10 periods, incl. evening 9–10). The period-time table remains fully user-editable (FR-9.1) so teachers at other schools can enter their own |

---

## 6. Functional Requirements

Priorities: **P0** = must ship, **P1** = should ship in v1.0, **P2** = later.

### FR-1 Semester & Academic Calendar (P0)
- **FR-1.1** Support multiple semesters; one is **active** at a time. Semester fields: name (e.g. "2026–2027学年第一学期"), week-1 Monday date, week-type ranges (teaching / exam / practice weeks), holidays, make-up workdays, notes.
- **FR-1.2** Compute and display the **current week number** everywhere (Today header, schedule, calendar): `weekNumber = floor((today − week1Monday) / 7) + 1`, clamped to ≥ 1.
- **FR-1.3** Show the semester phase for any date: **报到周 / 教学周 / 考试周 / 实践周 / 寒假（假期）** and holiday names (e.g. "国庆节 10/1–10/7 休").
- **FR-1.4** Holidays: a date-level list (start, end, name, type: 休/补). On a 休 date, scheduled classes are **suppressed and shown struck-through with a "放假" badge**; on a 补 make-up workday, the user can mark which weekday's timetable applies (a per-date override).
- **FR-1.5** Import the calendar **from a screenshot via LLM** (FR-3) into the FR-1.1 structure; everything remains manually editable (semester editor screen).

### FR-2 Course Timetable (P0)
- **FR-2.1** Course fields: name, course code (optional), day-of-week (1–7), period range (e.g. 1–4), week list (derived from a range like 2–17 plus optional odd/even pattern and manual exclusions), campus + room (e.g. 金牛校区·贡院401), teaching class name, **student count**, course nature (必修/选修, optional), exam type (考试/考查, optional), color (auto-assigned from palette).
- **FR-2.2** Today view: list of today's meetings sorted by start time, each showing period numbers, clock times, course, **class**, **student count**, room, and status (进行中 / 已结束 / 未开始 / 放假停课).
- **FR-2.3** Weekly grid: period rows generated dynamically from the PeriodSlot table (reference school: **10 rows** incl. evening) × 7 day columns, blocks spanning their period range, colored by course; current week selectable, swipe/arrow to other weeks; each week header shows **week number + date range**. Weeks outside the teaching range show the phase label (e.g. 考试周) instead of a grid.
- **FR-2.4** Full CRUD for courses with validation (no overlapping periods for the same day+weeks unless explicitly confirmed).
- **FR-2.5** Display conventions: dates as `M月D日 周X`; week badge as `第N周`.

### FR-3 LLM Screenshot Import (P0)
- **FR-3.1** Settings fields (BYOK): `Base URL` (OpenAI-compatible, e.g. `https://open.bigmodel.cn/api/paas/v4`), `API Key`, `Model` (default suggestion: a multimodal model such as `glm-4.5v` / `qwen-vl-max` / `gpt-4o`). Test-connection button.
- **FR-3.2** Two import flows sharing one engine: **导入课程表截图** and **导入校历截图**. User picks/captures an image → app downscales (long edge ≤ 2048 px) → sends base64 image + extraction prompt → receives strict JSON → opens a **confirmation page** listing parsed items with each field editable, add/delete rows → save.
- **FR-3.3** Extraction prompt requirements (embedded as a constant): explain Chinese educational-system timetable layout conventions (merged cells, 节次 ranges like "1-4节:2-17周", week patterns, campus/room lines, class-name lines, student counts); require **strict JSON only**, schema below; unknown fields → `null`, never invent values.
- **FR-3.4** Timetable JSON schema (array of courses):
  ```json
  { "name": "", "code": null, "dayOfWeek": 3, "periodStart": 1, "periodEnd": 4,
    "weeks": { "from": 2, "to": 17, "pattern": "all" },
    "campus": "金牛校区", "room": "贡院401",
    "className": "25级广播电视编导03班", "studentCount": 49,
    "nature": "必修", "examType": "考试" }
  ```
- **FR-3.5** Calendar JSON schema:
  ```json
  { "semesterName": "", "week1Monday": "2026-08-24",
    "teachingWeeks": { "from": 2, "to": 18 },
    "examWeeks": { "from": 19, "to": 20 }, "practiceWeeks": { "from": 21, "to": 21 },
    "holidays": [ { "name": "国庆节", "startDate": "2026-10-01", "endDate": "2026-10-07", "type": "rest" } ],
    "makeupWorkdays": [ { "date": "2026-10-10", "followsWeekday": null } ],
    "notes": [ "正式行课: 8月31日" ] }
  ```
- **FR-3.6** Robustness: 30 s timeout, 1 retry; on parse/validation failure show the raw error and offer manual entry; **never auto-save** — the confirmation page is mandatory. If no API key configured, route directly to manual entry with a hint.
- **FR-3.7** Privacy: images are sent **only** to the user-configured endpoint. Show the destination URL on the import screen. No telemetry, no other network calls.

### FR-4 Now-in-Class Progress (P0) — the core feature
- **FR-4.1** During a class meeting, the app shows a **frosted-glass progress card** (in-app, Today tab top + as a bottom-sheet from notifications):
  - Course ★ name, teaching class, room, period numbers.
  - **Class-end clock time (e.g. "下课后 10:10")** — always visible.
  - Progress bar with elapsed/remaining time, updated every second in-app; percentage; period-start/end times at both ends.
  - If between classes: "下一节 HH:MM · 课程名" mini-card. If no class today: friendly empty state.
- **FR-4.2** Visual: **glassmorphism** — blurred backdrop (Android `RenderEffect` blur, API 31+), ~60% translucent white surface, 24 dp rounded corners, hairline border, soft shadow, SF-Pro-like typography feel (use a clean sans like MiSans/Inter), subtle pulse dot when in class. **iPhone-native quality**; must also support dark mode.
- **FR-4.3** While a class is running, run a **foreground service** (`dataSync` or `specialUse` type, declared + justified) posting a **persistent notification**:
  - Media/progress style: on **targetSdk 36 use `Notification.ProgressStyle`** (Android 16); graceful fallback to `setProgress` on older devices.
  - Content: "影视声音创作 · 25级编导03班", "贡院401 · 下课 10:10", progress bar, remaining time; `visibility = PUBLIC` so it renders on the secure lock screen (mimicking the NetEase player card).
  - Tap → opens the app. `setOnlyAlertOnce(true)`. Updates every minute via in-service handler; no wake locks beyond what the service holds.
  - Auto-posts at period start (exact alarm) and auto-cancels at period end.
- **FR-4.4** The service must survive: `START_STICKY`, boot-completed receiver re-arming alarms, and battery-optimization exemption request (FR-5.3).

### FR-5 Notifications & Reliability (P0)
- **FR-5.1** **Class-end warning**: exact alarm at `periodEnd − 5 min` (configurable 1–15 min in Settings): heads-up notification + vibration "还有5分钟下课（10:10 下课）". Also an alarm at `periodEnd` ("下课时间到").
- **FR-5.2** Affairs reminders (FR-6) use exact alarms (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` on API 31+; `setExactAndAllowWhileIdle`). Default lead times, per-event selectable: 准时 / 提前10分钟 / 提前1小时 / 提前1天.
- **FR-5.3** Background survival on ColorOS: on first launch show a one-time **"保持提醒" setup guide** — request `POST_NOTIFICATIONS` (API 33+), battery-optimization exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), and step-by-step ColorOS instructions (自启动允许、电池→不限制、锁定后台). Re-check silently on each app open and show a gentle banner if revoked.
- **FR-5.4** `BOOT_COMPLETED` + `TIME_SET`/`TIMEZONE_CHANGED` receivers reschedule all pending alarms from the DB (single source of truth: scheduled occurrences are recomputed, not persisted alarm ids).

### FR-6 School-Affairs Calendar (P0)
- **FR-6.1** Month calendar view: each row labeled with its **week number**; day cells show dots for affairs + holidays; selected-day list below.
- **FR-6.2** Affair/event fields: title, date, optional time, optional place, note, reminder choice (none / on-time / 10 min / 1 h / 1 day before), optional color tag (会议/考试/事务/其他).
- **FR-6.3** The calendar merges three layers: **semester phases & holidays** (from FR-1), **class meetings** (FR-2), **affairs** (this FR). Day detail shows all three.
- **FR-6.4** CRUD for affairs; reminders fire via FR-5.2 alarms; reminder text includes title + time.

### FR-7 Teaching Stats (P1)
- **FR-7.1** Per semester, per course: meetings **held to date** (excluding holiday-suppressed), total planned, accumulated **学时** (periods × school rule: by default 学时 = periods count; configurable multiplier if the school counts differently), total students taught.
- **FR-7.2** A simple stats screen: this week X meetings / Y 学时; this semester totals; per-course bars. **No money anywhere.**

### FR-8 Data & Backup (P0)
- **FR-8.1** Local Room database; no account.
- **FR-8.2** **Export**: single JSON file (semesters, courses, affairs, settings minus API key) via Storage Access Framework + share sheet. **Import**: validate + replace (with confirm) or merge (add non-conflicting ids). Show schema version; forward-compatible.
- **FR-8.3** "Clear all data" in Settings with double confirmation.

### FR-9 Settings (P0)
- **FR-9.1** **Period-time table editor**: list of periods 1…N (support ≥ 12 rows to allow evening periods), each with label + start/end time; per-campus override optional (default: one shared table). **Ships with placeholder times (Appendix C) until the owner supplies the real 作息时间表.**
- **FR-9.2** LLM API (FR-3.1). **FR-9.3** Reminder defaults (FR-5.1). **FR-9.4** Battery-survival guide (FR-5.3). **FR-9.5** Export/import (FR-8). **FR-9.6** About (version, school info).

---

## 7. UX & Information Architecture

Bottom navigation, 5 tabs:

| Tab | Name | Content |
|-----|------|---------|
| 1 | **今日** | Header: `第N周 · 9月16日 星期三 · 教学周` (+ holiday badge). Frosted now-in-class card (FR-4.1). Today's meeting list. Today's affairs |
| 2 | **课表** | Weekly grid (FR-2.3) with week switcher |
| 3 | **日历** | Month calendar + merged day detail (FR-6), FAB → 新增事务 |
| 4 | **统计** | FR-7 |
| 5 | **我的** | Semester manager, import (课表/校历截图), settings & backup (FR-8/9) |

Design language: light **glassmorphism** over a soft neutral background (keep the warm pink `#ff4d6d` / cream `#fff0f3` family as the accent system, per the owner's earlier aesthetic), 20–24 dp rounded corners, restrained shadows, large bold numeric typography for times; dark theme derived from Material 3. Motion: subtle (card entrance, progress pulse) — 60 fps, no overdraw-heavy blur stacking.

---

## 8. Architecture & Tech Stack

| Concern | Choice (all "latest stable" at build time) |
|---------|--------------------------------------------|
| Language / UI | Kotlin 2.x, **Jetpack Compose + Material 3**, single-Activity |
| Async | Coroutines + Flow |
| DI | Hilt |
| Persistence | **Room** (entities in §9), DataStore for settings |
| Background | **Foreground Service** (in-class notification) + **AlarmManager exact alarms** (reminders) + WorkManager (deferrable rescheduling/backup helpers) |
| Networking | Ktor client or OkHttp + kotlinx.serialization (LLM API) |
| Images | Coil (pick/crop via Photo Picker) |
| Blur | `Modifier.blur` / `RenderEffect` (API 31+), graceful degrade below |
| Min/Target | minSdk 29, targetSdk 36, compileSdk 36 |
| Build | Gradle version catalog, AGP latest stable, R8 full mode, `debug` + `release` (self-signed) |
| Quality | JUnit + Turbine for logic (week calc, occurrence expansion, alarm scheduling), Compose UI tests for confirmation flow |

Package layout:
```
com.classbell.app
├─ core/ (time, weekcalc, result)
├─ data/ (room: entities, daos, repo, backup)
├─ llm/ (api client, prompts, schemas)
├─ service/ (InClassService, AlarmScheduler, receivers)
├─ ui/ (today, schedule, calendar, stats, settings, import, theme, glass/ components)
└─ di/
```

---

## 9. Data Model (Room)

```
Semester(id, name, week1Monday: LocalDate, teachingFrom, teachingTo,
         examFrom?, examTo?, practiceFrom?, practiceTo?,
         notes, isActive: Boolean)
Holiday(id, semesterId, name, startDate, endDate, type: REST|MAKEUP,
        followsWeekday: Int?)          // 补课日按周几课表, null=不确定
PeriodSlot(id, order: Int, label, startTime: LocalTime, endTime: LocalTime,
           campus: String?)             // null = 全校区通用
Course(id, semesterId, name, code?, dayOfWeek: 1-7, periodStart, periodEnd,
       weekFrom, weekTo, weekPattern: ALL|ODD|EVEN,
       weekExclusions: List<Int>, campus?, room,
       className, studentCount: Int, nature?, examType?, color: String)
Affair(id, title, date, time?, place?, note?, category,
       reminder: NONE|AT_TIME|MIN10|HOUR1|DAY1)
Override(id, semesterId, date, kind: CANCEL|SHIFT, followsWeekday: Int?)
        // 手动调课标记, 优先级高于 Holiday
Settings(key, value)                    // activeSemesterId, apiKey, baseUrl, model,
                                        // endWarnMinutes, defaultReminder, ...
```

---

## 10. Key Algorithms

### 10.1 Week number
```
weekOf(date) = floor((date - semester.week1Monday).days / 7) + 1   // both Mondays-normalized
phaseOf(week): week in teaching → 教学周; exam → 考试周; practice → 实践周; else 假期
```

### 10.2 Meeting occurrence expansion (pure function, heavily unit-tested)
```
meetingsOn(date):
  1. w = weekOf(date); if w < 1 → []
  2. eff = overrideFor(date) ?? holidayFor(date)
     if eff == CANCEL or (REST holiday and no SHIFT) → [] (but surface "放假停课" info)
     if SHIFT/MElLOW followsWeekday → substitute dayOfWeek
  3. courses where course.dayOfWeek == effectiveDay
     and w in [weekFrom..weekTo]
     and matchesPattern(w) and w not in weekExclusions
  4. join PeriodSlot for clock times → Meeting(course, date, start, end)
```

### 10.3 In-class state machine (drives FR-4/FR-5)
```
for each Meeting m of today:
  schedule exact alarms at m.start (show card+notification),
            m.end - warnMin (heads-up "还有N分钟"),
            m.end (cancel notification, "下课")
current = meetings.first { now in [start, end) }
next    = meetings.first { start > now }
progress = (now - start) / (end - start)
```
Alarms are recomputed on: boot, app open, time/timezone change, any data edit.

---

## 11. LLM Import — Prompt & Integration Spec

- Transport: `POST {baseUrl}/chat/completions`, OpenAI-compatible `messages` with `image_url` base64 data URI; `temperature: 0`; `response_format` JSON when supported; strip code fences otherwise; strict kotlinx.serialization parse.
- System prompt (zh, stored as asset): "你是教务系统课表/校历截图解析器…" — describes merged-cell conventions, period-range grammar (`1-4节:2-17周`, `5-8节,单周`), campus/room extraction (金牛校区 贡院401), class-name line, student count (人口 icon row), instructing: output **only JSON matching the schema**, `null` for unknown, never guess dates not shown; for calendars map 休→rest, 补→makeup with `followsWeekday: null` when not stated.
- Validation layer: dates ISO-parseable, `weekFrom ≤ weekTo`, `periodStart ≤ periodEnd`, dayOfWeek 1–7; invalid rows flagged (not silently dropped) on the confirmation page.
- Errors mapped to friendly copy: 401 → "API Key 无效"; 429 → "额度不足/限流"; timeout → retry hint.

---

## 12. Milestones

| Phase | Deliverable |
|-------|-------------|
| M0 | Project scaffold, theme, glass component kit, navigation shell |
| M1 | Data layer + semester/course/affair/period CRUD (manual) + week-number engine (unit-tested) |
| M2 | Today / Schedule / Calendar UI reading real DB, holiday suppression |
| M3 | In-class foreground service + ProgressStyle notification + end-warning exact alarms + survival guide (FR-5.3) |
| M4 | LLM import (timetable + calendar) with confirmation flow, BYOK settings |
| M5 | Stats screen, export/import backup, about |
| M6 | Release signing, R8, APK share sheet, on-device verification on OnePlus 12 / ColorOS 16 (lock-screen visibility after screen-off, alarm fire with app swiped away, reboot re-arm) |
| M7 (P2 backlog) | Home-screen AppWidget (today's classes + live progress), iOS? no — Android only; Excel import if needed |

---

## 13. Acceptance Criteria

- [ ] Import the two reference screenshots (Appendix B) → confirmation page shows exactly 2 courses / 1 semester config → saved → Today shows `第N周` correctly on any date in Sep 2026.
- [ ] With device locked and app swiped from recents, at period start the progress notification appears on the lock screen and ticks each minute; at end − 5 min a heads-up fires; at end it cancels.
- [ ] Reboot the phone → alarms still fire (boot receiver).
- [ ] Battery optimization exemption prompt shown once; after enabling ColorOS 自启动 + no battery limits, notifications survive 24 h idle.
- [ ] Holiday date (2026-10-07) shows no classes with 放假 badge; a manually created affair with 提前1天 reminder fires exactly 24 h early.
- [ ] Export → wipe app → import restores semesters/courses/affairs (API key intentionally excluded).
- [ ] Cold start < 800 ms on OnePlus 12; scrolling the semester grid is jank-free (Baseline Profile included).
- [ ] `assembleRelease` produces a shareable signed APK; opening it from WeChat/file manager installs.

---

## 14. Assumptions & Open Items

1. **✅ Resolved (2026-09-03): period-time table received** — see Appendix C for the authoritative seed times. Note the reference school has **evening periods 9–10**, so the schedule grid (FR-2.3) and Today list must render **all periods from the PeriodSlot table (default 10 rows)**, not a hard-coded 8.
2. Odd/even-week (单双周) courses are supported via `weekPattern` even though the owner's current courses are continuous (needed for colleagues).
3. Make-up workday semantics: the reference calendar marks 补 days (e.g. 2026-09-20, 2026-10-10) but does not state which weekday they follow; default = ask user per-date via the override editor (`followsWeekday`).
4. 学时 rule assumed `1 period = 1 学时` (FR-7.1 configurable).
5. App name "ClassBell" is provisional; single-string-resource rename.

---

## Appendix A — Reference Calendar Facts (2026–2027 Fall, Sichuan Film and Television University)

- Week 1 Monday: **2026-08-24** (教职工报到 8/24; 在校生报到 8/27–28; 新生报到 8/28; 新生入学教育 8/30; **正式行课 8/31**; 新生军训 9/14–27).
- **教学周: 2–18 · 期末复习/考试周: 19–20 · 学生专业实践周: 21**, thereafter 寒假.
- Holidays: **中秋节 2026-09-25 → 09-27**; **国庆节 2026-10-01 → 10-07**; **元旦节 2027-01-01 → 01-03**. Marked make-up workdays (补): 2026-09-20 (日), 2026-10-10 (六) — `followsWeekday` unverified.

## Appendix B — Seed Data (owner's current courses)

Both on **Wednesday (dayOfWeek=3), weeks 2–17, 金牛校区 贡院401, 必修, 考试, 理论 64 学时**:

| Course | Code | Periods | Class | Students |
|--------|------|---------|-------|----------|
| 影视声音创作★ | B090118 | 1–4 | 25级广播电视编导03班 | 49 |
| 影视声音创作★ | 2040109 | 5–8 | 25级摄影摄像技术01班 | 47 |

Ship these + Appendix A semester as an on-first-launch seed (skippable/delete-able), so day-0 UX is real.

## Appendix C — Period-Time Table (作息时间表, authoritative seed data)

Supplied by the owner on 2026-09-03. Reference school: **10 periods/day** (4 morning + 4 afternoon + 2 evening). Ship as first-launch seed; editable in FR-9.1 for teachers of other schools.

| Period | Start | End |
|--------|-------|-----|
| 1  | 08:30 | 09:15 |
| 2  | 09:20 | 10:05 |
| 3  | 10:25 | 11:10 |
| 4  | 11:15 | 12:00 |
| 5  | 14:30 | 15:15 |
| 6  | 15:25 | 16:10 |
| 7  | 16:20 | 17:05 |
| 8  | 17:15 | 18:00 |
| 9  | 19:00 | 19:45 |
| 10 | 19:55 | 20:40 |

Non-period rows (for reference only, **not** teaching periods, do not seed as PeriodSlots): 晨练 07:00–07:50; staff office hours 08:00–12:00 / 14:30–17:30.

Each period is 45 min; the editor should allow any times (other schools differ).

---
*End of document.*
