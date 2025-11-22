package com.hawk.codex

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class CodexToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 1. 创建主面板
        val panel = JPanel(BorderLayout())

        // 2. 添加一段提示文字
        val label = JLabel("开启codex之旅吧")
        label.horizontalAlignment = SwingConstants.CENTER
        label.preferredSize = Dimension(200, 50)
        panel.add(label, BorderLayout.NORTH)

        // 3. 创建启动按钮
        val startButton = JButton("打开 Codex Terminal")

        // 4. 核心逻辑：按钮点击事件
        startButton.addActionListener {
            try {
                // 获取 IDEA 的终端视图实例
                val terminalView = TerminalToolWindowManager.getInstance(project)

                // 创建一个新的 Shell 窗口，名称叫 "Codex"
                val widget = terminalView.createShellWidget(project.basePath, "Codex", true, false)

                // 执行命令。注意：你的电脑必须已经安装了 codex 命令
                // 如果 codex 不在环境变量里，这里需要写绝对路径，例如 "/usr/local/bin/codex"
                widget.sendCommandToExecute("codex")

            } catch (e: Exception) {
                e.printStackTrace()
                label.text = "启动失败: ${e.message}"
            }
        }

        panel.add(startButton, BorderLayout.CENTER)

        // 5. 将面板注入到工具窗口中
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}