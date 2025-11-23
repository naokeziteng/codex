package com.hawk.codex

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * 打开新的终端标签页并执行 codex 命令
 */
class OpenCodexTerminalAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            // 获取终端管理器
            val terminalManager = TerminalToolWindowManager.getInstance(project)

            // 创建新的终端标签页（每次都创建新的）
            val widget = terminalManager.createShellWidget(
                project.basePath,
                "codex",
                true,  // activate - 激活新创建的标签页
                false  // requestFocus - 不立即获取焦点
            )

            // 执行 codex 命令
            widget.sendCommandToExecute("codex")

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
