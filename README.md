# Codex - AI Code Completion for IntelliJ IDEA

一个集成 AI 代码补全和 Codex CLI 的 IntelliJ IDEA 插件。使用本地 Ollama 模型提供类似 GitHub Copilot 的代码补全体验，并支持 RAG（检索增强生成）。

## ✨ 功能特性

### 🤖 AI 代码补全
- **本地模型**: 基于 Ollama，无需网络，保护代码隐私
- **智能提示**: 使用 DeepSeek-Coder 等专业代码模型
- **上下文感知**: 理解光标前后代码，生成合适的补全
- **RAG 增强**: 从项目代码库检索相关代码，提升补全质量

### 🚀 快捷操作
- **快速触发**: `Option + \` (Mac) / `Alt + \` (Windows/Linux) 触发补全
- **快速接受**: `Tab` 键接受补全建议
- **右键发送**: 右键菜单 "Send to Codex" 快速发送文件/代码到终端

### 🔧 Codex CLI 集成
- **一键启动**: 右侧边栏按钮直接打开 Codex 终端
- **智能定位**: 发送文件时自动包含行号 (`@file.kt#L12-19`)

## 📦 安装

### 前置要求

1. **IntelliJ IDEA**
   - Community Edition 或 Ultimate Edition
   - 版本: 2025.1.4.1 或更高

2. **Ollama** ([下载地址](https://ollama.ai/))
   ```bash
   # macOS
   brew install ollama

   # Linux
   curl -fsSL https://ollama.ai/install.sh | sh

   # Windows
   # 访问 https://ollama.ai/download 下载安装包
   ```

3. **下载模型**
   ```bash
   # 启动 Ollama 服务
   ollama serve

   # 下载代码补全模型（推荐 6.7B）
   ollama pull deepseek-coder:6.7b

   # （可选）下载嵌入模型用于 RAG
   ollama pull nomic-embed-text
   ```

4. **Codex CLI** (可选，用于终端集成)
   ```bash
   # 安装 Codex CLI 工具
   # 参考: https://claude.ai/code
   ```

### 插件安装

#### 方式一：从源码构建

```bash
# 克隆项目
git clone https://github.com/your-username/codex.git
cd codex

# 构建插件
./gradlew buildPlugin

# 插件 ZIP 位于: build/distributions/codex-*.zip
```

在 IntelliJ IDEA 中安装：
1. `Settings` > `Plugins` > `⚙️` > `Install Plugin from Disk...`
2. 选择 `build/distributions/codex-*.zip`
3. 重启 IDE

#### 方式二：从 Marketplace 安装（即将推出）
*插件正在审核中，敬请期待*

## 🎯 使用指南

### 配置插件

1. 打开设置: `Settings` > `Tools` > `Codex Completion`
2. 配置选项:
   - **Ollama URL**: 默认 `http://localhost:11434`
   - **Completion Model**: 选择补全模型（如 `deepseek-coder:6.7b`）
   - **Embedding Model**: RAG 嵌入模型（如 `nomic-embed-text`）
   - **Enable Completion**: 启用代码补全
   - **Enable RAG**: 启用检索增强生成

### 代码补全

1. **触发补全**:
   - 按 `Option + \` (Mac) 或 `Alt + \` (Windows/Linux)
   - 灰色文本显示补全建议

2. **接受补全**:
   - 按 `Tab` 键接受建议
   - 按 `Esc` 键取消

3. **示例**:
   ```java
   public class Main {
       public static void main(String[] args) {
           // 写个冒泡排序
           [按 Option + \，模型会生成冒泡排序代码]
       }
   }
   ```

### Send to Codex

1. **发送文件**:
   - 在编辑器或项目视图中右键文件
   - 选择 "Send to Codex"
   - 终端会收到: `@/path/to/file.kt`

2. **发送代码段**:
   - 选中代码
   - 右键 > "Send to Codex"
   - 终端会收到: `@/path/to/file.kt#L12-19`

### Codex CLI 终端

- 点击右侧边栏的 "Codex" 按钮
- 自动在终端中启动 `codex` 命令

## ⚙️ 配置建议

### 模型选择

| 模型 | 速度 | 质量 | 内存占用 | 适用场景 |
|------|------|------|----------|----------|
| `deepseek-coder:1.3b` | ⚡⚡⚡ | ⭐⭐ | ~2GB | 快速简单补全 |
| `deepseek-coder:6.7b` | ⚡⚡ | ⭐⭐⭐⭐ | ~6GB | **推荐** 日常开发 |
| `codellama:13b` | ⚡ | ⭐⭐⭐⭐⭐ | ~12GB | 高质量补全 |

### 性能优化

1. **上下文长度**: 默认 1500 字符（约 50 行）
   - 小模型（1.3B）可减少到 800 字符
   - 大模型（13B）可增加到 2500 字符

2. **RAG 配置**:
   - 关闭 RAG 可提升响应速度（约快 30%）
   - 开启 RAG 可提升补全质量（尤其是项目特定代码）

3. **Ollama 优化**:
   ```bash
   # 设置 Ollama 环境变量（可选）
   export OLLAMA_NUM_PARALLEL=2  # 并行请求数
   export OLLAMA_MAX_LOADED_MODELS=2  # 最大加载模型数
   ```

## 🛠️ 开发

### 构建项目

```bash
# 编译
./gradlew build

# 运行测试 IDE
./gradlew runIde

# 构建插件 ZIP
./gradlew buildPlugin
```

### 调试

1. **查看 Ollama 请求**:
   ```bash
   # 测试 API
   curl http://localhost:11434/api/tags

   curl -X POST http://localhost:11434/api/generate \
     -d '{"model":"deepseek-coder:6.7b","prompt":"def hello():","stream":false}'
   ```

2. **查看 IntelliJ 日志**:
   - `Help` > `Show Log in Finder/Explorer`
   - 搜索 "Codex" 或 "Ollama"

### 技术栈

- **语言**: Kotlin 2.1.0
- **框架**: IntelliJ Platform SDK 2025.1
- **HTTP 客户端**: OkHttp 4.12.0
- **JSON 解析**: Gson 2.10.1
- **LLM 服务**: Ollama

## 📋 已知问题

1. **InlineCompletionProvider 在 CE 版本中不稳定**
   - 解决方案: 使用手动触发方式 (`Option + \`)

2. **小模型（1.3B）有时返回中文解释**
   - 解决方案: 升级到 6.7B 模型，或在代码中过滤中文字符

3. **首次补全较慢**
   - 原因: Ollama 需要加载模型到内存（约 3-5 秒）
   - 后续补全会快很多

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 项目
2. 创建特性分支: `git checkout -b feature/amazing-feature`
3. 提交更改: `git commit -m 'Add amazing feature'`
4. 推送分支: `git push origin feature/amazing-feature`
5. 提交 Pull Request

## 📄 许可证

MIT License

## 🙏 致谢

- [Ollama](https://ollama.ai/) - 本地 LLM 运行时
- [DeepSeek-Coder](https://github.com/deepseek-ai/DeepSeek-Coder) - 代码生成模型
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) - 插件开发框架

## 📞 联系方式

- Email: hawkchose1@gmail.com
- GitHub: [项目地址](https://github.com/your-username/codex)

---

**提示**: 如果遇到问题，请先查看 [CLAUDE.md](./CLAUDE.md) 中的常见问题解答。
