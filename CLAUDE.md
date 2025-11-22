# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 IntelliJ IDEA 插件项目,用于在 IDE 侧边栏集成 Codex CLI 工具。插件提供了一个工具窗口,允许用户通过按钮快速在内置终端中启动 Codex 命令行界面。

- **插件 ID**: com.hawk.codex
- **插件名称**: Codex
- **目标平台**: IntelliJ IDEA Community Edition 2025.1.4.1
- **最低构建版本**: 251
- **语言**: Kotlin (JVM 21)

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

**CodexToolWindowFactory** (`src/main/kotlin/com/hawk/codex/CodexToolWindowFactory.kt`)
- 实现 `ToolWindowFactory` 接口
- 创建包含启动按钮的侧边栏工具窗口
- 集成 IntelliJ Terminal 插件,在新终端会话中执行 `codex` 命令
- UI 使用 Swing (JPanel, JButton, JLabel)

### 插件配置

**plugin.xml** (`src/main/resources/META-INF/plugin.xml`)
- 声明插件元数据和依赖
- 注册工具窗口到右侧边栏 (anchor="right")
- 工具窗口 ID: "Codex CLI"
- 依赖模块: `com.intellij.modules.platform` 和 `org.jetbrains.plugins.terminal`

### 构建配置

**build.gradle.kts**
- 使用 IntelliJ Platform Gradle Plugin 2.7.1
- Kotlin 2.1.0
- Java/Kotlin 目标版本: JVM 21
- 依赖 IntelliJ Community Edition 和 Terminal 插件

## 关键技术细节

### Terminal 集成
插件使用 `TerminalToolWindowManager` API 创建终端会话:
```kotlin
val terminalView = TerminalToolWindowManager.getInstance(project)
val widget = terminalView.createShellWidget(project.basePath, "Codex", true, false)
widget.sendCommandToExecute("codex")
```

### 前置条件
- 系统必须已安装 `codex` 命令行工具
- `codex` 需要在 PATH 环境变量中,或在代码中使用绝对路径

### Gradle 配置优化
- 启用了配置缓存 (`org.gradle.configuration-cache = true`)
- 启用了构建缓存 (`org.gradle.caching = true`)
- 禁用了 Kotlin 标准库的默认依赖 (`kotlin.stdlib.default.dependency = false`)

## 文件结构
```
codex/
├── build.gradle.kts                    # 构建配置
├── gradle.properties                   # Gradle 属性
├── settings.gradle.kts                 # 项目设置
└── src/main/
    ├── kotlin/com/hawk/codex/
    │   └── CodexToolWindowFactory.kt   # 工具窗口实现
    └── resources/META-INF/
        └── plugin.xml                  # 插件描述符
```
