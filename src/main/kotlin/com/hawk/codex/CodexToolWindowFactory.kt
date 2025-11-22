package com.hawk.codex

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class CodexToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            // 获取 IDEA 的终端视图实例
            val terminalView = TerminalToolWindowManager.getInstance(project)

            // 创建一个新的 Shell 窗口，名称叫 "Codex"
            val widget = terminalView.createShellWidget(project.basePath, "Codex", true, false)

            // 执行命令
            widget.sendCommandToExecute("codex")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 隐藏工具窗口面板，只保留侧边栏按钮
        toolWindow.hide()
    }
}
