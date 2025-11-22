package com.hawk.codex.index

import com.hawk.codex.ollama.OllamaClient
import com.hawk.codex.settings.CodexSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 代码索引服务
 * 负责索引项目代码并提供检索功能
 */
@Service(Service.Level.PROJECT)
class CodeIndexService(private val project: Project) {

    private val logger = Logger.getInstance(CodeIndexService::class.java)
    private val vectorStore = VectorStore()
    private val executor = Executors.newSingleThreadExecutor()
    private val indexingFiles = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var isIndexing = false

    init {
        // 监听文件变化，自动更新索引
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach { event ->
                        val file = event.file ?: return@forEach
                        if (CodeChunker.shouldIndex(file.path)) {
                            reindexFile(file)
                        }
                    }
                }
            }
        )
    }

    /**
     * 索引整个项目
     */
    fun indexProject(onProgress: ((Int, Int) -> Unit)? = null) {
        if (isIndexing) {
            logger.info("Already indexing, skipping...")
            return
        }

        executor.submit {
            isIndexing = true
            try {
                val basePath = project.basePath ?: return@submit
                val baseDir = VirtualFileManager.getInstance()
                    .findFileByUrl("file://$basePath") ?: return@submit

                val files = collectFiles(baseDir)
                logger.info("Found ${files.size} files to index")

                files.forEachIndexed { index, file ->
                    try {
                        indexFile(file)
                        onProgress?.invoke(index + 1, files.size)
                    } catch (e: Exception) {
                        logger.warn("Failed to index ${file.path}", e)
                    }
                }

                logger.info("Indexing complete. Total chunks: ${vectorStore.size()}")
            } finally {
                isIndexing = false
            }
        }
    }

    /**
     * 索引单个文件
     */
    private fun indexFile(file: VirtualFile) {
        val filePath = file.path
        val basePath = project.basePath ?: return
        val relativePath = filePath.removePrefix(basePath).removePrefix("/")

        if (indexingFiles.contains(filePath)) return
        indexingFiles.add(filePath)

        try {
            val content = String(file.contentsToByteArray())
            val chunks = CodeChunker.chunkCode(content, relativePath)

            if (chunks.isEmpty()) return

            val settings = CodexSettings.getInstance()
            val client = OllamaClient(settings.ollamaUrl)

            // 移除旧的索引
            vectorStore.removeByFile(relativePath)

            // 为每个代码块生成嵌入并存储
            chunks.forEachIndexed { index, chunk ->
                try {
                    val vector = client.embed(chunk.toSearchText(), settings.embeddingModel)
                    val id = "${relativePath}:${chunk.startLine}-${chunk.endLine}"
                    vectorStore.add(id, chunk, vector)
                } catch (e: Exception) {
                    logger.debug("Failed to embed chunk $index of $relativePath: ${e.message}")
                }
            }
        } finally {
            indexingFiles.remove(filePath)
        }
    }

    /**
     * 重新索引文件（文件变化时调用）
     */
    private fun reindexFile(file: VirtualFile) {
        if (!CodexSettings.getInstance().enableRag) return

        executor.submit {
            try {
                indexFile(file)
            } catch (e: Exception) {
                logger.warn("Failed to reindex ${file.path}", e)
            }
        }
    }

    /**
     * 检索相关代码
     * @param query 查询文本（通常是当前编辑的代码上下文）
     * @param topK 返回数量
     */
    fun search(query: String, topK: Int = 5): List<CodeChunk> {
        if (vectorStore.size() == 0) return emptyList()

        return try {
            val settings = CodexSettings.getInstance()
            val client = OllamaClient(settings.ollamaUrl)
            val queryVector = client.embed(query, settings.embeddingModel)

            vectorStore.search(queryVector, topK)
                .map { it.first }
        } catch (e: Exception) {
            logger.warn("Search failed", e)
            emptyList()
        }
    }

    /**
     * 收集需要索引的文件
     */
    private fun collectFiles(dir: VirtualFile): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        collectFilesRecursive(dir, files)
        return files
    }

    private fun collectFilesRecursive(dir: VirtualFile, result: MutableList<VirtualFile>) {
        for (child in dir.children) {
            when {
                child.isDirectory -> {
                    // 跳过常见的忽略目录
                    val name = child.name
                    if (name !in IGNORED_DIRS) {
                        collectFilesRecursive(child, result)
                    }
                }
                CodeChunker.shouldIndex(child.path) -> {
                    result.add(child)
                }
            }
        }
    }

    /**
     * 获取索引状态
     */
    fun getStatus(): IndexStatus {
        return IndexStatus(
            isIndexing = isIndexing,
            indexedChunks = vectorStore.size(),
            indexedFiles = vectorStore.getIndexedFiles().size
        )
    }

    data class IndexStatus(
        val isIndexing: Boolean,
        val indexedChunks: Int,
        val indexedFiles: Int
    )

    companion object {
        private val IGNORED_DIRS = setOf(
            ".git", ".idea", ".gradle", "build", "out", "target",
            "node_modules", "__pycache__", ".venv", "venv",
            "dist", ".next", ".nuxt"
        )

        fun getInstance(project: Project): CodeIndexService {
            return project.getService(CodeIndexService::class.java)
        }
    }
}
