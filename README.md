# Timeless (时光律动)

<p align="left">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/Release-v2.4.0-orange?style=flat-square" alt="Release" />
</p>

面向高校教师和大学生的 Android 课表与日程应用，将课程、个人事项、提醒和计时放在一起。无广告，支持通过 AI 识别减少手动录入。

> **系统要求**：Android 8.0 及以上  
> **开源协议**：[GNU General Public License v3.0 (GPLv3)](./LICENSE)  
> [English Version below (跳转至下方英文版)](#english-version)

---

## 为什么做这个应用

我做 Timeless，是因为自己在教学和日常中经常遇到几个小麻烦：

- 上课时，需要反复解锁手机查看距离下课还有多久。
- 一些课表应用的广告会打断使用。
- 教师课表录入比较麻烦，调课后还需要重新整理。
- 课程、会议和个人事项分散在不同地方，不方便一起查看。

Timeless 是我尝试解决这些问题的工具。我希望它能让课程安排更容易查看，让日程维护少一些重复操作。

---

## 主要功能

### 1. 课表与日程
管理课程、会议和个人事项，并通过日历查看全天安排。支持**教师**与**学生**两种使用模式（教师模式聚焦授课班级、防拖堂提醒；学生模式聚焦教室门牌与任课教师）。

### 2. 上课提醒与课程计时
提供上课提醒、课程实时倒计时和下课前震动提醒。通过通知栏流体轨道和锁屏常驻卡片查看当前小节进度，减少反复解锁手机打开应用的需要。

### 3. 灵活调课与放假调休
针对高校频繁出现的临时调课、停课和节假日调休（如“周日补上周五课程”），支持单日成对调换、临时改时间或改教室，并保留调课备注。所有变动基于上层排程计算，不会破坏底层的原始学期总课表。

### 4. AI 辅助录入与本地快析
从课表图片、校历长图、公文截图和文本通知中自动识别日程内容，减少手动录入：
- **纯文本通知**：内置本地规则引擎，支持离线秒级快析（无需联网）。
- **图片与复杂公文**：使用云端大模型服务（目前接入智谱 AI）进行多模态识别。
- **自定义配置**：支持高级用户在设置中填入自己的 API Key（BYOK 模式）。

### 5. 写入系统日历
支持将 Timeless 中的课程与待办事项单向写入手机系统日历，方便在桌面日历插件或其他日历应用中聚合查看。
> **注意**：这不是双向同步。在系统日历中进行的修改不会自动同步回 Timeless。

### 6. 其他实用特性
- 跨时区时间智能识别与本地换算（方便跨国线上会议与学术交流）
- 结构化 JSON 数据一键备份与恢复

---

## 数据与隐私

课表和日程数据保存在设备本地（使用本地 SQLite 数据库），核心排程与日程查看功能完全支持离线使用。

但请注意：**AI 图片识别不是离线功能**。
- 使用图片或复杂智能识别时，待识别的内容会发送至云端 AI 服务处理，目前默认使用服务商为智谱。我不会查看或保存用户数据。
- “数据保存在本地”不代表智能识别过程中数据不会离开设备。云端服务商如何处理收到的内容，以其相关服务条款和隐私政策为准。

---

## 免费使用与服务费用

Timeless 目前完全无广告。

智能识别功能目前向用户免费开放，默认通道的 API 调用费用由我个人承担。当前用户数量较少，我能够负担这部分支出。如果未来使用量显著增加导致成本过高，我可能会调整免费调用额度或服务方式，届时会提前说明变化；这里不作无限量或永久免费承诺。

如需稳定高频使用，推荐在设置中配置个人自备的 API Key。

---

## 已测试的设备

目前，我仅在以下系统环境的实机上进行了实际测试：
1. **ColorOS**（OPPO / 一加设备，支持状态栏流体通知与常驻卡片）
2. **小米澎湃 OS (Xiaomi HyperOS)**

其他系统与厂商设备尚未经过我的全面实机验证。通知栏流体显示、锁屏卡片常驻与后台提醒的具体表现，可能因系统深度休眠策略和权限设置而有所差异。

---

## 关于开发

我的背景是声音设计和教学，而不是软件工程。Timeless 既是我为解决实际需求制作的工具，也是一次 AI 辅助开发实践。

我使用 Google Antigravity 作为主要 AI 编程工具参与开发。大部分代码由 AI 辅助生成，我的主要工作是构想功能、定义需求、评估结果、进行实机测试并推动迭代。

AI 辅助开发是这个项目的具体开发方式，不是对软件质量或可靠性的背书或保证。它仍然是一个在持续完善中的个人项目。

---

## 下载与反馈

### 下载安装
请在 GitHub Releases 页面查看并下载最新的正式版安装包（APK）：  
👉 [**Timeless Releases 下载页**](https://github.com/ccfooe/Timeless/releases)

### 缺陷反馈与贡献
本项目基于 **GNU General Public License v3.0 (GPLv3)** 开源。如果在使用中遇到任何问题，或有改进想法，欢迎在 Issues 中反馈或提交 Pull Request。

反馈缺陷时，请尽量提供以下信息：
- 手机型号与系统版本（如 Xiaomi 14, HyperOS 1.0）
- Timeless 版本号
- 复现问题的操作步骤
- 预期结果与实际现象
- 截图或录屏（如适用）

> ⚠️ **提交截图前，请务必遮盖姓名、学号、具体课程或任何不希望公开的个人隐私信息。**

---
---

# English Version

# Timeless

An Android timetable and schedule app for university teachers and students. It brings courses, personal affairs, reminders, and countdowns together into a single, ad-free view with AI-assisted schedule importing.

> **System Requirements**: Android 8.0 (API 26) or higher  
> **License**: [GNU General Public License v3.0 (GPLv3)](./LICENSE)

---

## Why I Built This

I built Timeless to solve a few practical issues I frequently encountered in teaching and daily campus life:

- In class, having to repeatedly unlock the phone just to check how many minutes were left.
- Intrusive splash-screen advertisements in common timetable apps.
- The tedium of entering faculty timetables, and the headache of rescheduling after changes.
- Courses, meetings, and personal tasks scattered across different tools without a unified overview.

Timeless is my tool to address these needs—making class schedules clearer and reducing repetitive daily maintenance.

---

## Key Features

### 1. Timetables & Schedules
Manage courses, meetings, and personal tasks in one unified view. Supports tailored **Faculty** and **Student** modes (Faculty mode focuses on class cohorts and anti-overtime vibration alerts; Student mode highlights classrooms and instructors).

### 2. Class Reminders & Countdowns
Provides class start alerts, live countdowns, and pre-end vibration reminders. Real-time class progress is visible right on the status bar and lock screen, minimizing the need to unlock your phone.

### 3. Rescheduling & Holiday Swaps
Built to handle campus rescheduling realities—cancelled sessions, make-up classes, room changes, and official holiday swaps (e.g., "Sunday runs Friday's timetable"). Changes apply without corrupting your baseline semester timetable.

### 4. AI Ingestion & Offline Fast Parsing
Import schedules from timetable photos, academic calendars, and school notices to cut down manual data entry:
- **Plain-text notices**: Handled instantly on-device via a built-in heuristic parser (no internet required).
- **Images and complex notices**: Parsed via multimodal cloud AI models (currently powered by Zhipu AI).
- **Custom API Key (BYOK)**: Advanced users can provide their own API credentials in Settings.

### 5. Export to System Calendar
Write Timeless courses and tasks into your device's system calendar for quick access via calendar widgets or third-party tools.
> **Note**: This is a one-way export, not a two-way sync. Modifications made inside the system calendar will not sync back to Timeless.

### 6. Additional Utilities
- Intelligent timezone detection and conversion for international meetings and academic webinars.
- One-click structured JSON data backup and restore.

---

## Data & Privacy

Timetable and schedule data are stored strictly on your local device (in a local SQLite database). Core scheduling and daily viewing features function entirely offline.

Please note: **AI image recognition is not an offline feature.**
- When you use image or complex smart ingestion, the relevant text/image is transmitted to a cloud AI provider (currently Zhipu AI) for processing. I do not access or store your data.
- "Data stored locally" does not mean data never leaves your device during cloud AI processing. The handling of transmitted data is governed by the respective cloud provider's terms and privacy policy.

---

## Free Usage & API Costs

Timeless is completely free of advertisements.

The cloud AI recognition feature is currently provided to users free of charge, with API expenses covered personally by me. With a modest user base, I am happy to sponsor these costs. Should usage scale significantly, free quotas or service tiers may need to be adjusted with prior notice. No unlimited or perpetual free service is promised.

Users with high-frequency needs are encouraged to configure their own API keys in Settings.

---

## Tested Devices

To date, physical on-device testing has only been conducted on:
1. **ColorOS** (OPPO / OnePlus, supports fluid cloud status notifications)
2. **Xiaomi HyperOS**

Other manufacturer ROMs and devices have not yet undergone my personal verification. Ongoing notification styles, lock-screen visibility, and background keep-alive behavior may vary depending on system battery optimization policies and permissions.

---

## About Development

My professional background is in sound design and university teaching rather than software engineering. Timeless is both a practical tool created for everyday needs and an experiment in AI-assisted development.

I use Google Antigravity as my primary AI coding assistant. Most code is generated with AI support; my role is to formulate ideas, define specifications, evaluate outputs, conduct physical testing, and guide revisions.

AI-assisted development describes how this project is made—it is not a guarantee of software quality or flawless reliability. It remains an evolving personal project.

---

## Download & Feedback

### Download
You can download the latest installable APK package from GitHub Releases:  
👉 [**Timeless Releases Page**](https://github.com/ccfooe/Timeless/releases)

### Feedback & Contributing
Timeless is open-sourced under the **GNU General Public License v3.0 (GPLv3)**. Bug reports, feature suggestions, and pull requests are warmly welcome.

When reporting an issue, please include:
- Device model and OS version (e.g., Xiaomi 14, HyperOS 1.0)
- Timeless version number
- Clear steps to reproduce the issue
- Expected vs. actual behavior
- Screenshots or screen recordings, if applicable

> ⚠️ **Before submitting screenshots, please redact your name, student ID, specific classes, or any sensitive personal information.**
