# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 IntelliJ IDEA 插件项目，集成了 AI 代码补全和 Codex CLI 工具。插件提供以下核心功能：

1. **AI 代码补全**: 基于通义千问 (Qwen) 云端 API 的智能代码补全
2. **Send to Codex**: 右键菜单快速发送文件/代码到 Codex CLI 终端
3. **终端集成**: 快速在内置终端中启动 Codex 命令行界面

- **插件 ID**: com.hawk.codex
- **插件名称**: Codex
- **目标平台**: IntelliJ IDEA Community Edition 2025.1.4.1
- **最低构建版本**: 251
- **语言**: Kotlin (JVM 21)
- **依赖**: 阿里云 DashScope API (通义千问)

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
- 调用 QwenClient 生成补全建议

**CodexCompletionAction** (`src/main/kotlin/com/hawk/codex/completion/CodexCompletionAction.kt`)
- 快捷键触发的手动补全 Action (Option/Alt + \)
- 使用 Inlay hints 显示灰色补全文本
- 处理补全文本的清理和过滤

**AcceptCompletionAction** (`src/main/kotlin/com/hawk/codex/completion/AcceptCompletionAction.kt`)
- Tab 键接受补全建议
- 将补全文本插入到光标位置

**QwenClient** (`src/main/kotlin/com/hawk/codex/qwen/QwenClient.kt`)
- 通义千问 API 客户端（基于 OkHttp）
- 使用 OpenAI 兼容模式调用 DashScope API
- 支持 FIM (Fill-in-Middle) 代码补全
- API 端点: `https://dashscope.aliyuncs.com/compatible-mode/v1/completions`

#### 2. Codex CLI 集成

**OpenCodexTerminalAction** (`src/main/kotlin/com/hawk/codex/OpenCodexTerminalAction.kt`)
- 工具栏按钮
- 点击直接在终端启动 `codex` 命令

**SendToCodexAction** (`src/main/kotlin/com/hawk/codex/SendToCodexAction.kt`)
- 右键菜单 Action
- 发送文件路径到终端: `@filepath`
- 选中代码时发送行号: `@filepath#L12-19`

#### 3. 设置管理

**CodexSettings** (`src/main/kotlin/com/hawk/codex/settings/CodexSettings.kt`)
- 应用级服务，存储插件配置
- 配置项:
  - API Key (DashScope API Key)
  - 补全模型 (默认: qwen3-coder-plus)
  - 最大 Token 数 (默认: 512)
  - Temperature (默认: 0.1)
  - 是否启用补全

**CodexSettingsConfigurable** (`src/main/kotlin/com/hawk/codex/settings/CodexSettingsConfigurable.kt`)
- 设置界面 UI
- 在 Tools > Codex Completion 中显示

### 插件配置

**plugin.xml** (`src/main/resources/META-INF/plugin.xml`)
- 声明插件元数据和依赖
- 注册的扩展点:
  - `applicationService`: CodexSettings
  - `applicationConfigurable`: 设置界面
  - `inlineCompletionProvider`: AI 补全提供者
- 注册的 Actions:
  - `OpenCodexTerminalAction`: 打开 Codex 终端
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
QwenClient.complete()
    - 构建 FIM prompt: "<|fim_prefix|>prefix<|fim_suffix|>suffix<|fim_middle|>"
    - 调用 DashScope API /compatible-mode/v1/completions
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

### 2. 通义千问 API 调用细节

**API 端点**:
- Base URL: `https://dashscope.aliyuncs.com/compatible-mode/v1`
- Completions: `/completions`

**认证**:
- Header: `Authorization: Bearer {DASHSCOPE_API_KEY}`

**FIM 格式**:
```
<|fim_prefix|>{prefix_code}<|fim_suffix|>{suffix_code}<|fim_middle|>
```

**补全请求**:
```json
{
  "model": "qwen3-coder-plus",
  "prompt": "<|fim_prefix|>def hello():\n    <|fim_suffix|>\n    return result<|fim_middle|>",
  "max_tokens": 512,
  "temperature": 0.1,
  "stream": false
}
```

**关键参数**:
- `temperature: 0.1`: 低温度，生成更确定的代码
- `max_tokens: 512`: 最多生成 512 tokens
- `stream: false`: 非流式响应

**可用模型**:
- `qwen3-coder-plus`: 推荐，支持上下文缓存
- `qwen3-coder-flash`: 更快，支持上下文缓存
- `qwen2.5-coder-32b-instruct`: 高质量
- `qwen2.5-coder-14b-instruct`
- `qwen2.5-coder-7b-instruct`: 入门选择
- `qwen-coder-turbo`: 阿里云托管优化版

### 3. Terminal 集成

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

### 4. 前置条件与环境要求

**必需**:
- 阿里云 DashScope API Key（可在 https://bailian.console.aliyun.com/?tab=ak#/api-key 获取）
- `codex` CLI 工具在 PATH 中（用于终端集成功能）

**API Key 获取步骤**:
1. 访问阿里云百炼控制台
2. 创建 API Key
3. 在插件设置中配置 API Key

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
    │   ├── OpenCodexTerminalAction.kt         # 打开终端 Action
    │   ├── SendToCodexAction.kt               # 右键菜单 Action
    │   ├── completion/
    │   │   ├── CodexInlineCompletionProvider.kt   # Inline 补全提供者
    │   │   ├── CodexCompletionAction.kt           # 手动触发补全
    │   │   └── AcceptCompletionAction.kt          # 接受补全
    │   ├── qwen/
    │   │   └── QwenClient.kt                  # 通义千问 API 客户端
    │   └── settings/
    │       ├── CodexSettings.kt               # 设置存储
    │       └── CodexSettingsConfigurable.kt   # 设置 UI
    └── resources/META-INF/
        └── plugin.xml                         # 插件描述符
```

## 开发注意事项

### 调试

1. **查看 API 请求日志**:
   ```bash
   # 在 QwenClient.kt 中添加日志
   println("Request: ${requestBody.toString()}")
   println("Response: $responseBody")
   ```

2. **查看 IntelliJ 日志**:
   - Help > Show Log in Finder/Explorer
   - 搜索 "Codex" 或 "Qwen"

3. **测试通义千问 API**:
   ```bash
   # 测试补全
   curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/completions \
     -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "model": "qwen3-coder-plus",
       "prompt": "<|fim_prefix|>def bubble_sort(arr):<|fim_suffix|>",
       "max_tokens": 512,
       "temperature": 0.1
     }'
   ```

### 常见问题

**问题 1: InlineCompletionProvider 不触发**
- IntelliJ CE 对 InlineCompletionProvider 支持有限
- 解决方案：使用手动触发 (CodexCompletionAction + Inlay hints)

**问题 2: API 401/403 错误**
- 检查 API Key 是否正确配置
- 确认 API Key 未过期

**问题 3: API 429 错误**
- 请求过于频繁，请稍后再试
- 考虑添加请求节流

**问题 4: 模型返回中文解释而不是代码**
- 在 `cleanCompletion()` 中过滤中文字符
- 使用更大的模型

**问题 5: Tab 键接受补全不工作**
- 不要用 AWT KeyListener（会被 IntelliJ 拦截）
- 使用 Action + keyboard-shortcut 注册
