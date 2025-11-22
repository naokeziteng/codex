package com.hawk.codex.completion

import com.hawk.codex.index.CodeIndexService
import com.hawk.codex.ollama.OllamaClient
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
 * 支持 RAG（检索增强生成）
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

        val editor = request.editor
        val document = request.document
        val offset = request.endOffset
        val project = editor.project

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
                val client = OllamaClient(settings.ollamaUrl)

                // RAG: 检索相关代码
                var ragContext = ""
                if (settings.enableRag && project != null) {
                    try {
                        val indexService = CodeIndexService.getInstance(project)
                        // 使用光标附近的代码作为查询
                        val queryText = trimmedPrefix.takeLast(500)
                        val relevantChunks = indexService.search(queryText, topK = 3)

                        if (relevantChunks.isNotEmpty()) {
                            ragContext = buildRagContext(relevantChunks)
                        }
                    } catch (e: Exception) {
                        logger.debug("RAG search failed: ${e.message}")
                    }
                }

                // 构建增强的 prefix
                val enhancedPrefix = if (ragContext.isNotBlank()) {
                    "// Related code from project:\n$ragContext\n\n// Current file:\n$trimmedPrefix"
                } else {
                    trimmedPrefix
                }

                client.complete(
                    prefix = enhancedPrefix,
                    suffix = trimmedSuffix,
                    model = settings.completionModel
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
     * 构建 RAG 上下文
     */
    private fun buildRagContext(chunks: List<com.hawk.codex.index.CodeChunk>): String {
        return chunks.joinToString("\n\n") { chunk ->
            "// From: ${chunk.filePath}:${chunk.startLine}-${chunk.endLine}\n${chunk.content}"
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

        // 移除 EOT 标记
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

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        val settings = CodexSettings.getInstance()
        return settings.enabled
    }
}
