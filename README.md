# Timeless (时光律动)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="License" />
  <img src="https://img.shields.io/badge/Release-v2.4.0-orange?style=for-the-badge" alt="Release" />
  <img src="https://img.shields.io/badge/Built_with-Antigravity_AI-purple?style=for-the-badge" alt="Antigravity" />
</p>

<p align="center">
  <strong>极简高校智能作息与全景日程助手 · 纯粹、克制、永无广告、100% 离线数据主权</strong><br>
  <em>A minimalist, ad-free campus timetable & countdown companion with 100% on-device privacy.</em>
</p>

---

## 🌟 创作者宣言 (Creator's Story)

> **“我本身一行代码也不会写。这款软件是我利用 Google DeepMind 的 AI Agent 工具 Antigravity，通过 Engineering Vibe Coding（自然语言工程对话）从零到一完整构想、研发并测试上线的。”**  
> —— **李超 (Chao Li)**, 独立创作者 / AI Vibe Coder

在商业课表工具充斥着开屏摇一摇广告、贷款推销与冗杂社交的今天，Timeless 诞生于对数字时代过度干扰的克制反思。这是一个完全由 **AI 原生协作** 打磨出的工业级原生应用：它拥有 100% 测试覆盖的排程纯函数计算引擎、流畅的 Material 3 动画、Android 16 级锁屏流体进度卡片，以及真正的端侧离线智能。

---

## ✨ 核心特性 (Key Features)

### 1. 教师 / 学生双模式智能自适应 (Dual Identity Adaptive Modes)
* **同一套课表，身份随心切**：
  * **教师视角**：自动聚合授课班级、教室位置，在下课前智能强力震动提醒（防拖堂神器）；
  * **学生视角**：一眼聚焦教学楼、教室门牌与任课老师，自习与备考无缝接入。

### 2. 状态栏与锁屏流体进度卡片 (Lockscreen & Status Bar Countdown)
* **抬手即知下课节拍**：
  * 状态栏灵动胶囊常驻毫秒级同步倒计时；
  * 锁屏与通知栏呈现现代流体进度轨（高对比白色实心进度 + 镂空细底轨），上课还剩几分钟，一目了然；
  * Android 16 Live Updates 与各大厂商（ColorOS 流体云、OriginOS 原生动态）硬件级协议兼容。

### 3. 全能智能免密导入 (All-in-One Smart AI Ingest)
* **告别一节节手动录入的折磨**：
  * 拍一张学校教务课表、校历长图，或教务处调课公文截图，秒级全自动解析；
  * **Timeless 官方智能云通道**：开箱即用，国内秒级直连，免自备 API Key；
  * **BYOK 极客模式**：支持自定义接入 OpenAI GPT-4o、Google Gemini、阿里通义千问等任意兼容端点。

### 4. 100% 本地离线隐私与数据主权 (100% On-Device Privacy)
* **零追踪、零打点、零泄露**：
  * 课表、日程、备注 100% 仅存储在手机本地 SQLite 数据库中；
  * 无任何第三方广告、统计追踪或监控 SDK；
  * 支持一键导出标准 JSON 备份，自由换机迁移。

### 5. 跨时区与全景日程联动 (Timezone Conversion & Calendar Sync)
* **全球学术出访无缝换算**：
  * 智能识别跨国 Zoom/Teams 会议时区（如 London BST/GMT），自动换算为手机本地时间；
  * 事项与课程一键静默双向同步至系统日历。

---

## 📱 截图展示 (Screenshots)

| 首页与灵动倒计时 | 智能多模态录入 | 全景日程与校历 | 极致关于与设置 |
| :---: | :---: | :---: | :---: |
| *(待上传实机图)* | *(待上传实机图)* | *(待上传实机图)* | *(待上传实机图)* |

---

## 🚀 极速下载与安装 (Download)

* 稳定发布版本可直接前往 [Releases 页面](https://github.com/ccfooe/Timeless/releases) 下载最新 APK：
  * **当前版本**：`v2.4.0 (Build 50)`
  * **安装包**：`Timeless-v2.4.0.apk` (约 13 MB)
* 支持系统：Android 8.0 (API 26) 及更高版本。

---

## 🛠️ 技术栈与工程架构 (Architecture)

Timeless 严格遵从现代 Android 开发最佳实践与 **Engineering Vibe Coding** 规范：

* **开发语言**：Kotlin 2.0+
* **UI 框架**：Jetpack Compose (BOM) + Material 3
* **架构模式**：MVI (Model-View-Intent) + 领域驱动无副作用计算引擎 (`OccurrenceEngine`)
* **本地持久化**：AndroidX Room (SQLite) + Jetpack DataStore Preferences
* **后台保活调度**：Android Foreground Service (`InClassService`) + `AlarmManager`
* **网络与解析**：OkHttp3 + Kotlinx Serialization + 本地启发式快析引擎 (`FastLocalParser`)
* **质量门禁**：100% 单元测试覆盖发布门禁 (`./gradlew testReleaseUnitTest`)

---

## 📄 开源许可证 (License)

本项目采用 **[GNU General Public License v3.0 (GPLv3)](./LICENSE)** 开源协议。

* 允许免费学习、研究与个人使用；
* 任何衍生作品或代码修改在发布时必须同样保持开源；
* **严禁闭源商用或恶意打包植入广告**。

---

## 👨‍💻 独立创作者 (Creator)

* **独立设计与研发**：李超 (Chao Li)
* **GitHub**：[@ccfooe](https://github.com/ccfooe)
* **灵感与开发工具**：Google DeepMind **Antigravity**
