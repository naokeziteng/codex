package com.hawk.codex.index

/**
 * 代码块
 * @param content 代码内容
 * @param filePath 文件路径
 * @param startLine 起始行号
 * @param endLine 结束行号
 */
data class CodeChunk(
    val content: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int
) {
    /**
     * 生成用于检索的文本表示
     */
    fun toSearchText(): String {
        return "File: $filePath\nLines: $startLine-$endLine\n$content"
    }
}

/**
 * 代码分割器
 * 将源代码文件分割成适合嵌入的代码块
 */
object CodeChunker {

    private const val MAX_CHUNK_SIZE = 500 // 最大字符数
    private const val OVERLAP_LINES = 2    // 重叠行数

    /**
     * 按函数/类分割代码
     * 简单实现：按空行分割，并限制大小
     */
    fun chunkCode(content: String, filePath: String): List<CodeChunk> {
        val lines = content.lines()
        val chunks = mutableListOf<CodeChunk>()

        var currentChunk = StringBuilder()
        var startLine = 1
        var currentLine = 1

        for (line in lines) {
            currentChunk.appendLine(line)

            // 检查是否达到分割点
            val shouldSplit = when {
                // 达到最大大小
                currentChunk.length >= MAX_CHUNK_SIZE -> true
                // 遇到空行且已有一定内容
                line.isBlank() && currentChunk.length > 100 -> true
                // 遇到函数/类定义结束
                line.trim() == "}" && currentChunk.length > 50 -> true
                else -> false
            }

            if (shouldSplit && currentChunk.isNotBlank()) {
                chunks.add(
                    CodeChunk(
                        content = currentChunk.toString().trim(),
                        filePath = filePath,
                        startLine = startLine,
                        endLine = currentLine
                    )
                )

                // 保留重叠行
                val recentLines = lines.subList(
                    maxOf(0, currentLine - OVERLAP_LINES),
                    currentLine
                )
                currentChunk = StringBuilder()
                recentLines.forEach { currentChunk.appendLine(it) }
                startLine = maxOf(1, currentLine - OVERLAP_LINES + 1)
            }

            currentLine++
        }

        // 添加最后一个块
        if (currentChunk.isNotBlank()) {
            chunks.add(
                CodeChunk(
                    content = currentChunk.toString().trim(),
                    filePath = filePath,
                    startLine = startLine,
                    endLine = lines.size
                )
            )
        }

        return chunks
    }

    /**
     * 获取支持的文件扩展名
     */
    fun getSupportedExtensions(): Set<String> {
        return setOf(
            "kt", "java", "py", "js", "ts", "tsx", "jsx",
            "go", "rs", "cpp", "c", "h", "hpp",
            "swift", "rb", "php", "scala", "cs"
        )
    }

    /**
     * 检查文件是否应该被索引
     */
    fun shouldIndex(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "")
        return extension in getSupportedExtensions()
    }
}
