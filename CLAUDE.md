# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 IntelliJ IDEA 插件项目，集成了 AI 代码补全和 Codex CLI 工具。插件提供以下核心功能：

1. **AI 代码补全**: 基于本地 Ollama 模型的智能代码补全，支持 RAG（检索增强生成）
2. **Send to Codex**: 右键菜单快速发送文件/代码到 Codex CLI 终端
3. **终端集成**: 快速在内置终端中启动 Codex 命令行界面

- **插件 ID**: com.hawk.codex
- **插件名称**: Codex
- **目标平台**: IntelliJ IDEA Community Edition 2025.1.4.1
- **最低构建版本**: 251
- **语言**: Kotlin (JVM 21)
- **依赖**: Ollama (本地 LLM 服务)

## 构建与开发命令

### 基础构建任务
```bash
# 构建项目
./gradlew build

# 清理构建产物
./gradlew clean

# 仅编译类文件
./gradlew classes

# 编译测试类
./gradlew testClasses
```

### 插件开发任务
```bash
# 在沙箱环境中运行插件
./gradlew runIde

# 构建插件 ZIP 包(用于分发)
./gradlew buildPlugin

# 准备沙箱环境
./gradlew prepareSandbox

# 修补 plugin.xml 文件
./gradlew patchPluginXml

# 构建可搜索选项索引
./gradlew buildSearchableOptions

# 验证插件配置
./gradlew verifyPluginProjectConfiguration

# 验证插件结构
./gradlew verifyPluginStructure

# 验证插件与 IDE 版本的兼容性
./gradlew verifyPlugin
```

### 测试任务
```bash
# 运行测试
./gradlew test

# 准备测试环境
./gradlew prepareTest

# 运行性能测试
./gradlew testIdePerformance
```

## 架构概览

### 核心组件

#### 1. AI 代码补全系统

**CodexInlineCompletionProvider** (`src/main/kotlin/com/hawk/codex/completion/CodexInlineCompletionProvider.kt`)
- 实现 `InlineCompletionProvider` 接口
- 提供类似 GitHub Copilot 的 inline 补全体验
- 支持 RAG：从代码索引中检索相关代码片段作为上下文
- 调用 OllamaClient 生成补全建议

**CodexCompletionAction** (`src/main/kotlin/com/hawk/codex/completion/CodexCompletionAction.kt`)
- 快捷键触发的手动补全 Action (Option/Alt + \)
- 使用 Inlay hints 显示灰色补全文本
- 处理补全文本的清理和过滤

**AcceptCompletionAction** (`src/main/kotlin/com/hawk/codex/completion/AcceptCompletionAction.kt`)
- Tab 键接受补全建议
- 将补全文本插入到光标位置

**OllamaClient** (`src/main/kotlin/com/hawk/codex/ollama/OllamaClient.kt`)
- Ollama API 客户端（基于 OkHttp）
- 支持代码补全和文本嵌入
- API endpoints:
  - `/api/generate`: 代码补全
  - `/api/embeddings`: 生成向量嵌入
  - `/api/tags`: 获取可用模型列表

#### 2. RAG (检索增强生成) 系统

**CodeIndexService** (`src/main/kotlin/com/hawk/codex/index/CodeIndexService.kt`)
- 项目级服务，负责代码索引和检索
- 使用 VFS 监听器自动索引代码变更
- 支持语义搜索（基于向量相似度）

**VectorStore** (`src/main/kotlin/com/hawk/codex/index/VectorStore.kt`)
- 内存向量数据库
- 使用余弦相似度进行语义搜索
- 存储代码块的文本内容和向量嵌入

**CodeChunker** (`src/main/kotlin/com/hawk/codex/index/CodeChunker.kt`)
- 将代码文件切分为可索引的块
- 按函数/类/方法边界切分
- 支持 Java, Kotlin, Python, JavaScript 等语言

#### 3. Codex CLI 集成

**CodexToolWindowFactory** (`src/main/kotlin/com/hawk/codex/CodexToolWindowFactory.kt`)
- 右侧边栏工具窗口
- 点击按钮直接在终端启动 `codex` 命令

**SendToCodexAction** (`src/main/kotlin/com/hawk/codex/SendToCodexAction.kt`)
- 右键菜单 Action
- 发送文件路径到终端: `@filepath`
- 选中代码时发送行号: `@filepath#L12-19`

#### 4. 设置管理

**CodexSettings** (`src/main/kotlin/com/hawk/codex/settings/CodexSettings.kt`)
- 应用级服务，存储插件配置
- 配置项:
  - Ollama URL (默认: http://localhost:11434)
  - 补全模型 (默认: deepseek-coder:6.7b)
  - 嵌入模型 (默认: nomic-embed-text)
  - 是否启用补全
  - 是否启用 RAG

**CodexSettingsConfigurable** (`src/main/kotlin/com/hawk/codex/settings/CodexSettingsConfigurable.kt`)
- 设置界面 UI
- 在 Tools > Codex Completion 中显示

### 插件配置

**plugin.xml** (`src/main/resources/META-INF/plugin.xml`)
- 声明插件元数据和依赖
- 注册的扩展点:
  - `toolWindow`: Codex 工具窗口
  - `applicationService`: CodexSettings
  - `projectService`: CodeIndexService
  - `applicationConfigurable`: 设置界面
  - `inlineCompletionProvider`: AI 补全提供者
- 注册的 Actions:
  - `SendToCodexAction`: 发送到 Codex 终端
  - `CodexCompletionAction`: 触发补全 (Option/Alt + \)
  - `AcceptCompletionAction`: 接受补全 (Tab)
- 依赖模块:
  - `com.intellij.modules.platform`
  - `org.jetbrains.plugins.terminal`

### 构建配置

**build.gradle.kts**
- 使用 IntelliJ Platform Gradle Plugin 2.7.1
- Kotlin 2.1.0
- Java/Kotlin 目标版本: JVM 21
- 依赖:
  - IntelliJ Community Edition
  - Terminal 插件
  - OkHttp 4.12.0 (HTTP 客户端)
  - Gson 2.10.1 (JSON 解析)

## 关键技术细节

### 1. AI 代码补全流程

```
用户按 Option + \ 触发
    ↓
CodexCompletionAction.actionPerformed()
    ↓
获取光标前后上下文 (各 1500 字符)
    ↓
[可选] RAG: CodeIndexService.search() 检索相关代码
    ↓
OllamaClient.complete()
    - 构建 prompt: "prefix<CURSOR>suffix"
    - 添加 system prompt 指导模型行为
    - 调用 Ollama API /api/generate
    ↓
cleanCompletion() 清理结果
    - 移除 FIM 标记
    - 提取 markdown 代码块
    - 过滤中文解释
    ↓
显示 Inlay hint (灰色文本)
    ↓
用户按 Tab → AcceptCompletionAction 插入代码
```

### 2. Ollama API 调用细节

**补全请求**:
```json
{
  "model": "deepseek-coder:6.7b",
  "prompt": "代码上下文<CURSOR>光标后代码",
  "system": "You are a code completion assistant...",
  "stream": false,
  "options": {
    "temperature": 0.1,
    "num_predict": 512
  }
}
```

**关键参数**:
- `temperature: 0.1`: 低温度，生成更确定的代码
- `num_predict: 512`: 最多生成 512 tokens（约 256 行代码）
- `system`: 指示模型只输出代码，不要解释

**已知问题**:
- `max_tokens` 参数不被支持 → 使用 `num_predict`
- `suffix`, `raw` 参数会导致 400 错误 → 已移除
- DeepSeek-Coder 1.3B 模型上下文理解较弱 → 建议使用 6.7B

### 3. RAG 实现

**索引流程**:
```
项目打开 → CodeIndexService 初始化
    ↓
扫描所有代码文件
    ↓
CodeChunker 切分代码块 (按函数/类边界)
    ↓
OllamaClient.embed() 生成向量嵌入
    ↓
VectorStore 存储 (内存)
    ↓
VFS Listener 监听文件变更 → 增量更新索引
```

**检索流程**:
```
获取光标附近 500 字符作为查询
    ↓
OllamaClient.embed() 生成查询向量
    ↓
VectorStore.search() 余弦相似度搜索
    ↓
返回 Top-3 相关代码块
    ↓
拼接到 prompt: "// Related code:\n{chunks}\n\n// Current file:\n{prefix}"
```

### 4. Terminal 集成

**启动 Codex CLI**:
```kotlin
val terminalView = TerminalToolWindowManager.getInstance(project)
val widget = terminalView.createShellWidget(project.basePath, "Codex", true, false)
widget.sendCommandToExecute("codex")
```

**发送文件到终端**:
```kotlin
// 发送文件路径
ttyConnector.write("@${virtualFile.path}\n")

// 发送代码段（带行号）
val startLine = document.getLineNumber(selectionStart) + 1
val endLine = document.getLineNumber(selectionEnd) + 1
ttyConnector.write("@${virtualFile.path}#L$startLine-$endLine\n")
```

### 5. 前置条件与环境要求

**必需**:
- Ollama 服务运行在 localhost:11434
- 已下载代码模型（如 `deepseek-coder:6.7b`）
- （可选）嵌入模型用于 RAG（如 `nomic-embed-text`）
- `codex` CLI 工具在 PATH 中

**模型选择建议**:
- **deepseek-coder:1.3b**: 速度快（1-2秒），上下文理解弱，内存占用低（~2GB）
- **deepseek-coder:6.7b**: 平衡选项（3-5秒），推荐，内存占用中（~6GB）
- **codellama:13b**: 质量高（5-10秒），内存占用高（~12GB）

### Gradle 配置优化
- 启用了配置缓存 (`org.gradle.configuration-cache = true`)
- 启用了构建缓存 (`org.gradle.caching = true`)
- 禁用了 Kotlin 标准库的默认依赖 (`kotlin.stdlib.default.dependency = false`)

## 文件结构
```
codex/
├── build.gradle.kts                           # 构建配置
├── gradle.properties                          # Gradle 属性
├── settings.gradle.kts                        # 项目设置
└── src/main/
    ├── kotlin/com/hawk/codex/
    │   ├── CodexToolWindowFactory.kt          # 工具窗口
    │   ├── SendToCodexAction.kt               # 右键菜单 Action
    │   ├── completion/
    │   │   ├── CodexInlineCompletionProvider.kt   # Inline 补全提供者
    │   │   ├── CodexCompletionAction.kt           # 手动触发补全
    │   │   └── AcceptCompletionAction.kt          # 接受补全
    │   ├── ollama/
    │   │   └── OllamaClient.kt                # Ollama API 客户端
    │   ├── index/
    │   │   ├── CodeIndexService.kt            # 代码索引服务
    │   │   ├── VectorStore.kt                 # 向量数据库
    │   │   └── CodeChunker.kt                 # 代码分块器
    │   └── settings/
    │       ├── CodexSettings.kt               # 设置存储
    │       └── CodexSettingsConfigurable.kt   # 设置 UI
    └── resources/META-INF/
        └── plugin.xml                         # 插件描述符
```

## 开发注意事项

### 调试

1. **查看 Ollama 请求日志**:
   ```bash
   # 在 OllamaClient.kt 中添加日志
   println("Request: ${requestBody.toString()}")
   println("Response: $responseBody")
   ```

2. **查看 IntelliJ 日志**:
   - Help > Show Log in Finder/Explorer
   - 搜索 "Codex" 或 "Ollama"

3. **测试 Ollama API**:
   ```bash
   # 检查服务状态
   curl http://localhost:11434/api/tags

   # 测试补全
   curl -X POST http://localhost:11434/api/generate \
     -d '{"model":"deepseek-coder:6.7b","prompt":"def bubble_sort(arr):","stream":false}'
   ```

### 常见问题

**问题 1: InlineCompletionProvider 不触发**
- IntelliJ CE 对 InlineCompletionProvider 支持有限
- 解决方案：使用手动触发 (CodexCompletionAction + Inlay hints)

**问题 2: Ollama 400/500 错误**
- 检查参数是否支持（不要用 `max_tokens`, `suffix`, `raw`）
- 使用 `num_predict` 而不是 `max_tokens`

**问题 3: 模型返回中文解释而不是代码**
- 添加更强的 system prompt
- 使用更大的模型（6.7B 而不是 1.3B）
- 在 `cleanCompletion()` 中过滤中文字符

**问题 4: Tab 键接受补全不工作**
- 不要用 AWT KeyListener（会被 IntelliJ 拦截）
- 使用 Action + keyboard-shortcut 注册

**问题 5: RAG 索引速度慢**
- 限制索引文件类型（只索引代码文件）
- 使用增量索引（VFS Listener）
- 考虑持久化向量到磁盘
