# Codex - AI Code Completion for IntelliJ IDEA

一个集成 AI 代码补全和 Codex CLI 的 IntelliJ IDEA 插件。使用阿里云通义千问 (Qwen) API 提供类似 GitHub Copilot 的代码补全体验。

## ✨ 功能特性

### 🤖 AI 代码补全
- **云端 API**: 基于通义千问 qwen3-coder-plus 模型，无需本地 GPU
- **智能提示**: 专业代码模型，理解多种编程语言
- **上下文感知**: 理解光标前后代码，生成合适的补全
- **低延迟**: 云端 API 响应快速稳定

### 🚀 快捷操作
- **快速触发**: `Option + \` (Mac) / `Alt + \` (Windows/Linux) 触发补全
- **快速接受**: `Tab` 键接受补全建议
- **右键发送**: 右键菜单 "Send to Codex" 快速发送文件/代码到终端

### 🔧 Codex CLI 集成
- **一键启动**: 工具栏按钮直接打开 Codex 终端
- **智能定位**: 发送文件时自动包含行号 (`@file.kt#L12-19`)

## 📦 安装

### 前置要求

1. **IntelliJ IDEA**
   - Community Edition 或 Ultimate Edition
   - 版本: 2025.1.4.1 或更高

2. **阿里云 DashScope API Key**
   - 访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/?tab=ak#/api-key) 获取
   - 新用户有免费额度

3. **Codex CLI** (可选，用于终端集成)
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
   - **API Key**: 输入 DashScope API Key
   - **Completion Model**: 选择补全模型（默认 `qwen3-coder-plus`）
   - **Max Tokens**: 最大生成 Token 数（默认 512）
   - **Temperature**: 生成随机性（默认 0.1，越低越确定）
   - **Enable Completion**: 启用代码补全

3. 点击"测试连接"验证 API Key 是否有效

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

- 点击工具栏的 "Codex" 按钮
- 自动在终端中启动 `codex` 命令

## ⚙️ 配置建议

### API Key 获取

1. 访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/?tab=ak#/api-key)
2. 登录/注册阿里云账号
3. 创建 API Key
4. 复制 API Key 到插件设置中

### 模型选择

| 模型 | 特点 | 适用场景 |
|------|------|----------|
| `qwen3-coder-plus` | 高质量，支持上下文缓存 | **推荐** 日常开发 |

### 参数调优

- **Max Tokens**: 控制生成长度
  - 简单补全: 256
  - 完整函数: 512
  - 大段代码: 1024

- **Temperature**: 控制随机性
  - 0.1: 确定性高，适合代码补全
  - 0.3: 略有变化，适合创意代码
  - 0.7+: 高随机性，不推荐用于代码

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

1. **测试 API**:
   ```bash
   curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
     -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "model": "qwen3-coder-plus",
       "messages": [{"role": "user", "content": "Complete: def hello():"}],
       "max_tokens": 100
     }'
   ```

2. **查看 IntelliJ 日志**:
   - `Help` > `Show Log in Finder/Explorer`
   - 搜索 "Codex" 或 "Qwen"

### 技术栈

- **语言**: Kotlin 2.1.0
- **框架**: IntelliJ Platform SDK 2025.1
- **HTTP 客户端**: OkHttp 4.12.0
- **JSON 解析**: Gson 2.10.1
- **LLM 服务**: 阿里云 DashScope (通义千问)

## 📋 已知问题

1. **InlineCompletionProvider 在 CE 版本中不稳定**
   - 解决方案: 使用手动触发方式 (`Option + \`)

2. **模型有时返回中文解释**
   - 解决方案: 插件会自动过滤中文字符

3. **网络延迟**
   - 原因: 云端 API 调用需要网络
   - 建议: 确保网络稳定

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

- [阿里云 DashScope](https://dashscope.aliyun.com/) - 通义千问 API 服务
- [Qwen](https://github.com/QwenLM/Qwen) - 通义千问大模型
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) - 插件开发框架

## 📞 联系方式

- Email: hawkchose1@gmail.com
- GitHub: [项目地址](https://github.com/your-username/codex)

---

**提示**: 如果遇到问题，请先查看 [CLAUDE.md](./CLAUDE.md) 中的常见问题解答。
