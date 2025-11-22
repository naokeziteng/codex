package com.hawk.codex

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class SendToCodexAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // 获取相对路径
        val basePath = project.basePath ?: return
        val relativePath = file.path.removePrefix(basePath).removePrefix("/")
        val textToSend = "@$relativePath "

        // 查找或创建 codex 终端并发送文本
        sendToClaudeCodeTerminal(project, textToSend)
    }

    private fun sendToClaudeCodeTerminal(project: Project, text: String) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")

        if (toolWindow == null) {
            // 终端工具窗口不存在，创建新终端
            createAndSend(project, text)
            return
        }

        // 查找名为 "codex" 的终端 tab
        val contents = toolWindow.contentManager.contents
        for (content in contents) {
            if (content.displayName == "codex") {
                val widget = TerminalToolWindowManager.getWidgetByContent(content)
                if (widget != null) {
                    // 激活终端窗口
                    toolWindow.activate {
                        toolWindow.contentManager.setSelectedContent(content)
                        // 发送文本到终端（不执行）
                        typeTextToJBTerminal(widget, text)
                    }
                    return
                }
            }
        }

        // 没找到 codex 终端，创建新的
        createAndSend(project, text)
    }

    private fun createAndSend(project: Project, text: String) {
        try {
            val terminalManager = TerminalToolWindowManager.getInstance(project)
            val widget = terminalManager.createShellWidget(project.basePath, "codex", true, true)

            // 执行 codex 命令启动 codex
            widget.sendCommandToExecute("codex")

            // 延迟发送文本，等待 codex 启动
            com.intellij.util.Alarm().addRequest({
                // 通过 ttyConnector 写入文本
                try {
                    widget.ttyConnector?.write(text)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }, 2000)

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun typeTextToJBTerminal(widget: JBTerminalWidget, text: String) {
        try {
            // 通过 ttyConnector 写入文本
            widget.ttyConnector?.write(text)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun update(e: AnActionEvent) {
        // 只有在有文件选中时才显示
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = e.project != null && file != null && !file.isDirectory
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
