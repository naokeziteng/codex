package com.hawk.codex.completion

import com.hawk.codex.qwen.QwenClient
import com.hawk.codex.settings.CodexSettings
import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Codex Inline 代码补全提供者
 * 实现类似 GitHub Copilot 的 inline 补全体验
 * 使用通义千问 API
 */
class CodexInlineCompletionProvider : InlineCompletionProvider {

    private val logger = Logger.getInstance(CodexInlineCompletionProvider::class.java)

    override val id: InlineCompletionProviderID
        get() = InlineCompletionProviderID("CodexInlineCompletion")

    override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val settings = CodexSettings.getInstance()

        // 检查是否启用
        if (!settings.enabled) {
            return InlineCompletionSuggestion.Empty
        }

        // 检查 API Key
        if (settings.apiKey.isBlank()) {
            return InlineCompletionSuggestion.Empty
        }

        val document = request.document
        val offset = request.endOffset

        // 获取光标前后的代码作为上下文
        val prefix = document.text.substring(0, offset)
        val suffix = document.text.substring(offset)

        // 限制上下文长度
        val maxContextLength = 1500
        val trimmedPrefix = if (prefix.length > maxContextLength) {
            prefix.takeLast(maxContextLength)
        } else {
            prefix
        }
        val trimmedSuffix = if (suffix.length > maxContextLength) {
            suffix.take(maxContextLength)
        } else {
            suffix
        }

        return try {
            val completion = withContext(Dispatchers.IO) {
                val client = QwenClient(
                    apiKey = settings.apiKey,
                    model = settings.completionModel,
                    maxTokens = settings.maxTokens,
                    temperature = settings.temperature
                )

                client.complete(
                    prefix = trimmedPrefix,
                    suffix = trimmedSuffix
                )
            }

            if (completion.isNotBlank()) {
                val cleanedCompletion = cleanCompletion(completion)
                if (cleanedCompletion.isNotBlank()) {
                    InlineCompletionSingleSuggestion.build {
                        emit(InlineCompletionGrayTextElement(cleanedCompletion))
                    }
                } else {
                    InlineCompletionSuggestion.Empty
                }
            } else {
                InlineCompletionSuggestion.Empty
            }
        } catch (e: Exception) {
            logger.warn("Codex completion failed", e)
            InlineCompletionSuggestion.Empty
        }
    }

    /**
     * 清理补全结果
     */
    private fun cleanCompletion(completion: String): String {
        var result = completion

        // 移除 Qwen FIM 特殊标记
        result = result.replace("<|fim_prefix|>", "")
        result = result.replace("<|fim_suffix|>", "")
        result = result.replace("<|fim_middle|>", "")

        // 移除其他常见的 FIM 标记
        result = result.replace("<｜fim▁begin｜>", "")
        result = result.replace("<｜fim▁hole｜>", "")
        result = result.replace("<｜fim▁end｜>", "")
        result = result.replace("<|EOT|>", "")
        result = result.replace("<|eot_id|>", "")
        result = result.replace("<|endoftext|>", "")

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

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        val settings = CodexSettings.getInstance()
        return settings.enabled && settings.apiKey.isNotBlank()
    }
}
