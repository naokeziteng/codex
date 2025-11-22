package com.hawk.codex.completion

import com.hawk.codex.ollama.OllamaClient
import com.hawk.codex.settings.CodexSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import java.util.concurrent.Executors

/**
 * 代码补全 Action
 * 快捷键触发，显示 Inlay 补全建议
 */
class CodexCompletionAction : AnAction() {

    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        val COMPLETION_INLAY_KEY = Key.create<Inlay<*>>("codex.completion.inlay")
        val COMPLETION_TEXT_KEY = Key.create<String>("codex.completion.text")

        fun clearCompletion(editor: Editor) {
            val inlay = editor.getUserData(COMPLETION_INLAY_KEY)
            if (inlay != null && inlay.isValid) {
                Disposer.dispose(inlay)
            }
            editor.putUserData(COMPLETION_INLAY_KEY, null)
            editor.putUserData(COMPLETION_TEXT_KEY, null)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val settings = CodexSettings.getInstance()

        if (!settings.enabled) return

        // 清除之前的补全
        clearCompletion(editor)

        val document = editor.document
        val offset = editor.caretModel.offset

        // 获取上下文
        val prefix = document.text.substring(0, offset)
        val suffix = document.text.substring(offset)

        val maxContextLength = 1500
        val trimmedPrefix = if (prefix.length > maxContextLength) prefix.takeLast(maxContextLength) else prefix
        val trimmedSuffix = if (suffix.length > maxContextLength) suffix.take(maxContextLength) else suffix

        // 异步获取补全
        executor.submit {
            try {
                val client = OllamaClient(settings.ollamaUrl)
                val completion = client.complete(
                    prefix = trimmedPrefix,
                    suffix = trimmedSuffix,
                    model = settings.completionModel
                )

                val cleanedCompletion = cleanCompletion(completion)

                if (cleanedCompletion.isNotBlank()) {
                    ApplicationManager.getApplication().invokeLater {
                        showCompletion(editor, cleanedCompletion)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * 显示补全建议
     */
    private fun showCompletion(editor: Editor, completion: String) {
        val offset = editor.caretModel.offset

        // 创建 Inlay 显示补全
        val renderer = CompletionRenderer(completion)
        val inlay = editor.inlayModel.addInlineElement(offset, true, renderer)

        if (inlay != null) {
            editor.putUserData(COMPLETION_INLAY_KEY, inlay)
            editor.putUserData(COMPLETION_TEXT_KEY, completion)
        }
    }

    /**
     * 清理补全结果
     */
    private fun cleanCompletion(completion: String): String {
        var result = completion

        // 移除 FIM 特殊标记
        result = result.replace("<｜fim▁begin｜>", "")
        result = result.replace("<｜fim▁hole｜>", "")
        result = result.replace("<｜fim▁end｜>", "")
        result = result.replace("<|EOT|>", "")
        result = result.replace("<|eot_id|>", "")

        // 如果包含 markdown 代码块，提取代码块内容
        val codeBlockRegex = Regex("```\\w*\\n([\\s\\S]*?)```")
        val codeBlockMatch = codeBlockRegex.find(result)
        if (codeBlockMatch != null) {
            result = codeBlockMatch.groupValues[1]
        }

        // 移除开头的中文字符和标点
        val chinesePunctuation = setOf(
            '\uFF0C', '\u3002', '\uFF1A', '\uFF1B', '\uFF01', '\uFF1F',
            '\u3001', '\u201C', '\u201D', '\u2018', '\u2019', '\uFF08', '\uFF09'
        )
        result = result.dropWhile { char ->
            char.code in 0x4e00..0x9fa5 ||  // 中文字符
            char in chinesePunctuation ||    // 中文标点
            char.isWhitespace()
        }

        // 移除末尾空白
        result = result.trimEnd()

        return result
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null && CodexSettings.getInstance().enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * 补全渲染器 - 显示灰色文本
     */
    private class CompletionRenderer(private val text: String) : EditorCustomElementRenderer {

        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            val editor = inlay.editor
            val fontMetrics = editor.contentComponent.getFontMetrics(
                editor.colorsScheme.getFont(EditorFontType.PLAIN)
            )
            // 只计算第一行的宽度
            val firstLine = text.lines().firstOrNull() ?: text
            return fontMetrics.stringWidth(firstLine)
        }

        override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
            val editor = inlay.editor
            val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)

            g.font = font
            g.color = Color.GRAY

            val fontMetrics = g.fontMetrics
            val x = targetRegion.x
            val y = targetRegion.y + fontMetrics.ascent

            // 只绘制第一行
            val firstLine = text.lines().firstOrNull() ?: text
            g.drawString(firstLine, x, y)
        }
    }
}
