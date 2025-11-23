package com.hawk.codex

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import javax.swing.JButton
import javax.swing.JPanel
import java.awt.BorderLayout

class CodexToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建一个简单的面板，包含一个按钮
        val panel = JPanel(BorderLayout())
        val button = JButton("Open Codex Terminal")

        button.addActionListener {
            // 每次点击都创建新的终端标签页
            openCodexTerminal(project)
        }

        panel.add(button, BorderLayout.CENTER)

        // 将面板添加到工具窗口
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // 首次打开时自动创建一个终端
        openCodexTerminal(project)
    }

    private fun openCodexTerminal(project: Project) {
        try {
            // 获取终端管理器
            val terminalManager = TerminalToolWindowManager.getInstance(project)

            // 创建新的终端标签页
            val widget = terminalManager.createShellWidget(
                project.basePath,
                "Codex",
                true,   // activate - 激活新创建的标签页
                false   // requestFocus - 不立即获取焦点
            )

            // 执行 codex 命令
            widget.sendCommandToExecute("codex")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
