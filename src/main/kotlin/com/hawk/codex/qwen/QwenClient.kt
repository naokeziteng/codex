package com.hawk.codex.qwen

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 通义千问 API 客户端
 * 使用 OpenAI 兼容模式调用 DashScope API
 * 使用 Chat Completions API 实现代码补全
 */
class QwenClient(
    private val apiKey: String,
    private val model: String = "qwen3-coder-plus",
    private val maxTokens: Int = 512,
    private val temperature: Double = 0.1
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    companion object {
        private const val BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

        /**
         * 可用的 Qwen Coder 模型列表
         */
        val AVAILABLE_MODELS = listOf(
            "qwen3-coder-plus"
        )
    }

    /**
     * 代码补全
     * 使用 Chat Completions API + partial 模式实现
     * @param prefix 光标前的代码
     * @param suffix 光标后的代码
     * @return 补全结果
     */
    fun complete(prefix: String, suffix: String): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("API Key 未配置，请在设置中配置 DashScope API Key")
        }

        // 构建 messages
        val messages = JsonArray().apply {
            // System message
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", """You are a code completion assistant.
Complete the code at the [CURSOR] position.
IMPORTANT: Only output the code that should be inserted at the cursor position.
Do not include markdown code blocks, language tags, or explanations.
Do not repeat existing code.
Continue the code naturally based on the context.""")
            })
            // User message with context
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", "Complete the code at [CURSOR]:\n$prefix[CURSOR]$suffix")
            })
            // Assistant message with partial=true for prefix continuation
            add(JsonObject().apply {
                addProperty("role", "assistant")
                addProperty("content", "")
                addProperty("partial", true)
            })
        }

        val requestBody = JsonObject().apply {
            addProperty("model", model)
            add("messages", messages)
            addProperty("max_tokens", maxTokens)
            addProperty("temperature", temperature)
            addProperty("stream", false)
        }

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = parseErrorMessage(responseBody, response.code)
                throw IOException(errorMessage)
            }

            val json = gson.fromJson(responseBody, JsonObject::class.java)

            // Chat Completions 格式的响应: choices[0].message.content
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val choice = choices[0].asJsonObject
                val message = choice.getAsJsonObject("message")
                return message?.get("content")?.asString ?: ""
            }

            return ""
        }
    }

    /**
     * 验证 API Key 是否有效
     * 通过发送一个简单的 chat 请求来测试
     */
    fun isAvailable(): Boolean {
        if (apiKey.isBlank()) {
            return false
        }

        return try {
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", "hi")
                })
            }

            val requestBody = JsonObject().apply {
                addProperty("model", model)
                add("messages", messages)
                addProperty("max_tokens", 1)
            }

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(jsonMediaType))
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
        return AVAILABLE_MODELS
    }

    /**
     * 解析错误消息
     */
    private fun parseErrorMessage(responseBody: String, code: Int): String {
        return try {
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val error = json.getAsJsonObject("error")
            val message = error?.get("message")?.asString ?: "未知错误"
            when (code) {
                401 -> "API Key 无效或已过期"
                403 -> "没有访问权限，请检查 API Key"
                429 -> "请求过于频繁，请稍后再试"
                500, 502, 503 -> "服务器暂时不可用，请稍后再试"
                else -> "API 错误 ($code): $message"
            }
        } catch (e: Exception) {
            "API 错误 ($code): $responseBody"
        }
    }
}
