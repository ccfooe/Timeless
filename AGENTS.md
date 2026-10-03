# ClassBell Agent 工程开发守则 (AGENTS.md)

> **适用范围**：所有在 `Project_ClassBell` 代码库中进行开发协作的 AI 编码助手（Antigravity、Cursor、Claude Code、Codex 等）。
> **核心使命**：践行 **Engineering Vibe Coding**，拒绝代码泥球化，保持高可靠性与可维护性。

---

## 1. 项目概况与技术栈基底

* **项目名称**：ClassBell (Timeless / 极简高校智能作息与课表助手)
* **设计风格**：ColorOS / OriginOS 现代极简设计美学（灵动胶囊、沉浸毛玻璃卡片、微交互）
* **核心技术栈**：
  * **语言**：Kotlin 2.0+
  * **UI 框架**：Jetpack Compose (BOM) + Material 3
  * **架构模式**：MVI / MVVM + 纯领域无副作用计算引擎（`OccurrenceEngine`）
  * **数据库**：AndroidX Room (SQLite)
  * **异步通信**：Kotlin Coroutines & StateFlow / SharedFlow
  * **后台服务与调度**：Android Foreground Service (`InClassService`), `AlarmManager`
  * **AI 与解析**：智谱多模态大模型 (`GLM-4v-plus`) + 本地离线快析 (`FastLocalParser`)

---

## 2. 必备知识库与前置文件索引

在执行任何开发或解答前，必须查阅：
1. [`GLOSSARY.md`](./GLOSSARY.md)：**领域统一词汇表**。严禁私自生造字段名称（例如小节序号必须用 `periodOrder`，禁止使用 `slotOrder`；小节文案必须用 `periodLabel`）。
2. [`.planning/findings.md`](./.planning/findings.md)：**历史踩坑与技术约束清单**。记录了前台服务防重入、双向成对调课原则等核心经验。
3. [`.planning/task_plan.md`](./.planning/task_plan.md)：**当前任务规划与进度检查项**。

---

## 3. 命令行与沙箱执行铁律 (CRITICAL)

* **JDK 17 环境约束**：
  * macOS 本地 JDK 17 可配置于 `$HOME/.jdk/jdk-17/Contents/Home`。
  * **所有 `./gradlew` 编译、测试、打包命令必须指定 `BypassSandbox: true`**，否则将在沙箱中因找不到 Java 运行时报错。
* **常用基准命令**：
  ```bash
  # 运行发布版单元测试 (必须绿灯)
  ./gradlew testReleaseUnitTest
  
  # 编译发布版 Release APK
  ./gradlew assembleRelease
  ```

---

## 4. Engineering Vibe Coding 五大铁律

### 铁律 ①：需求先审问 (Grilling First)
* 当用户提出一个涉及新特性、多文件改动或复杂业务逻辑（如调课、放假、闹钟提醒）的请求时，**严禁直接敲代码**。
* 必须先像苏格拉底问诊一样，提出 3~6 个关键问题，区分“临时会话还是改库”、“静音还是删数据”、“目标日还是被借日”，与用户达成 100% 共识后再动手。

### 铁律 ②：严格遵从领域语言 (Ubiquitous Language)
* 代码、注释、日志、UI 提示中的术语必须与 [`GLOSSARY.md`](file:///Users/lichao/Project_ClassBell/GLOSSARY.md) 完全一致。
* 调课必须成对维护：目标执行日为 `SHIFT`，原课程日为 `CANCEL`（附带调离备注）。
* 临时用户免打扰必须走 `SessionSuppression`（内存级），绝不破坏底层数据库。

### 铁律 ③：测试先行与快速反馈 (TDD & Feedback Loops)
* 遇到算法、解析器（`FastLocalParser`）、排程计算（`OccurrenceEngine`）或边界条件缺陷时，优先编写红灯单元测试，重现 Bug 后再编码修复，确保绿灯通过。
* 严禁无验证的“碰运气式修改 (Shotgun Debugging)”。

### 铁律 ④：深模块设计，拒绝泥球架构 (Deep Modules)
* UI 界面组件（Composable 函数、Screen、Card）只负责渲染与用户意图派发，严禁在 UI 树中堆积大量复杂计算。
* 将排程运算、状态转换、文本清洗下沉到纯 Kotlin 的纯函数/领域模块中，提供极简的公共 API，隐藏内部复杂度。

### 铁律 ⑤：确定性交付门禁 (Completion Gate)
* 任何功能宣告开发完成前，必须执行三步自检：
  1. 执行 `./gradlew testReleaseUnitTest` 确保 100% 测试通过；
  2. 执行 `./gradlew assembleRelease` 确保无编译/混淆错误并生成 APK；
  3. 更新 [`.planning/task_plan.md`](file:///Users/lichao/Project_ClassBell/.planning/task_plan.md) 与 [`.planning/progress.md`](file:///Users/lichao/Project_ClassBell/.planning/progress.md)。
