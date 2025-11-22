package com.hawk.codex.ollama

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ollama API 客户端
 * 支持代码补全和 embedding 生成
 */
class OllamaClient(
    private val baseUrl: String = "http://localhost:11434"
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    /**
     * FIM (Fill-in-Middle) 代码补全
     * @param prefix 光标前的代码
     * @param suffix 光标后的代码
     * @param model 模型名称
     * @param onToken 流式回调，每次收到 token 时调用
     * @return 完整的补全结果
     */
    fun complete(
        prefix: String,
        suffix: String,
        model: String = "deepseek-coder:6.7b",
        onToken: ((String) -> Unit)? = null
    ): String {
        // 构建 system prompt，指示模型只输出代码
        val systemPrompt = """You are a code completion assistant.
Complete the code directly at the cursor position.
IMPORTANT: You are completing code INSIDE an existing file. Do NOT create new classes or methods unless the context requires it.
Only output the code that should be inserted at the cursor.
Do not include markdown code blocks, language tags, or explanations.
Do not repeat existing code.
Continue the code naturally based on the context."""

        // 构建 prompt，包含光标位置标记
        val prompt = "$prefix<CURSOR>$suffix"

        val requestBody = JsonObject().apply {
            addProperty("model", model)
            addProperty("prompt", prompt)
            addProperty("system", systemPrompt)
            addProperty("stream", false)
            add("options", JsonObject().apply {
                addProperty("temperature", 0.1)
                addProperty("num_predict", 512)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        val result = StringBuilder()

        if (onToken != null) {
            // 流式响应
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Ollama API error: ${response.code}")
                }

                response.body?.source()?.let { source ->
                    val reader = BufferedReader(source.inputStream().reader())
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            try {
                                val json = gson.fromJson(line, JsonObject::class.java)
                                val token = json.get("response")?.asString ?: ""
                                if (token.isNotEmpty()) {
                                    result.append(token)
                                    onToken(token)
                                }
                            } catch (e: Exception) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
            }
        } else {
            // 非流式响应
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Ollama API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                result.append(json.get("response")?.asString ?: "")
            }
        }

        return result.toString()
    }

    /**
     * 生成文本嵌入向量
     * @param text 要嵌入的文本
     * @param model embedding 模型
     * @return 向量数组
     */
    fun embed(text: String, model: String = "nomic-embed-text"): FloatArray {
        val requestBody = JsonObject().apply {
            addProperty("model", model)
            addProperty("prompt", text)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/embeddings")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Ollama embedding API error: ${response.code}")
            }

            val responseBody = response.body?.string() ?: ""
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val embeddingArray = json.getAsJsonArray("embedding")

            return FloatArray(embeddingArray.size()) { i ->
                embeddingArray[i].asFloat
            }
        }
    }

    /**
     * 检查 Ollama 服务是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/tags")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取可用的模型列表
     */
    fun listModels(): List<String> {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return emptyList()
            }

            val responseBody = response.body?.string() ?: ""
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val models = json.getAsJsonArray("models") ?: return emptyList()

            return models.mapNotNull { model ->
                model.asJsonObject.get("name")?.asString
            }
        }
    }
}
