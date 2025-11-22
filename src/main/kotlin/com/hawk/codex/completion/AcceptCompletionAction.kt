package com.hawk.codex.completion

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer

/**
 * 接受补全 Action
 * Tab 键触发
 */
class AcceptCompletionAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val completion = editor.getUserData(CodexCompletionAction.COMPLETION_TEXT_KEY) ?: return
        val inlay = editor.getUserData(CodexCompletionAction.COMPLETION_INLAY_KEY)

        // 清除 inlay
        if (inlay != null && inlay.isValid) {
            Disposer.dispose(inlay)
        }
        editor.putUserData(CodexCompletionAction.COMPLETION_INLAY_KEY, null)
        editor.putUserData(CodexCompletionAction.COMPLETION_TEXT_KEY, null)

        // 插入补全文本
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.insertString(editor.caretModel.offset, completion)
            editor.caretModel.moveToOffset(editor.caretModel.offset + completion.length)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasCompletion = editor?.getUserData(CodexCompletionAction.COMPLETION_TEXT_KEY) != null
        e.presentation.isEnabledAndVisible = hasCompletion
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
