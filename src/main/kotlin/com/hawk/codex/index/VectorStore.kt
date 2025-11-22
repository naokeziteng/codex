package com.hawk.codex.index

import kotlin.math.sqrt

/**
 * 向量存储条目
 */
data class VectorEntry(
    val id: String,
    val chunk: CodeChunk,
    val vector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VectorEntry
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * 简单的内存向量存储
 * 使用余弦相似度进行检索
 */
class VectorStore {

    private val entries = mutableMapOf<String, VectorEntry>()

    /**
     * 添加向量条目
     */
    fun add(id: String, chunk: CodeChunk, vector: FloatArray) {
        entries[id] = VectorEntry(id, chunk, vector)
    }

    /**
     * 批量添加
     */
    fun addAll(items: List<Triple<String, CodeChunk, FloatArray>>) {
        items.forEach { (id, chunk, vector) ->
            add(id, chunk, vector)
        }
    }

    /**
     * 删除指定文件的所有条目
     */
    fun removeByFile(filePath: String) {
        val toRemove = entries.values
            .filter { it.chunk.filePath == filePath }
            .map { it.id }
        toRemove.forEach { entries.remove(it) }
    }

    /**
     * 检索最相似的代码块
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @param minScore 最小相似度阈值
     */
    fun search(
        queryVector: FloatArray,
        topK: Int = 5,
        minScore: Float = 0.5f
    ): List<Pair<CodeChunk, Float>> {
        if (entries.isEmpty()) return emptyList()

        return entries.values
            .map { entry ->
                val score = cosineSimilarity(queryVector, entry.vector)
                entry.chunk to score
            }
            .filter { it.second >= minScore }
            .sortedByDescending { it.second }
            .take(topK)
    }

    /**
     * 计算余弦相似度
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    /**
     * 获取条目数量
     */
    fun size(): Int = entries.size

    /**
     * 清空所有数据
     */
    fun clear() {
        entries.clear()
    }

    /**
     * 获取所有索引的文件路径
     */
    fun getIndexedFiles(): Set<String> {
        return entries.values.map { it.chunk.filePath }.toSet()
    }
}
