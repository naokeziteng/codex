package com.hawk.codex.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Codex 插件设置
 * 持久化存储用户配置
 */
@Service(Service.Level.APP)
@State(
    name = "CodexSettings",
    storages = [Storage("codex-settings.xml")]
)
class CodexSettings : PersistentStateComponent<CodexSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var ollamaUrl: String = "http://localhost:11434",
        var completionModel: String = "deepseek-coder:6.7b",
        var embeddingModel: String = "nomic-embed-text",
        var maxTokens: Int = 256,
        var temperature: Double = 0.2,
        var debounceMs: Long = 300,
        var enableRag: Boolean = true
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var enabled: Boolean
        get() = state.enabled
        set(value) { state.enabled = value }

    var ollamaUrl: String
        get() = state.ollamaUrl
        set(value) { state.ollamaUrl = value }

    var completionModel: String
        get() = state.completionModel
        set(value) { state.completionModel = value }

    var embeddingModel: String
        get() = state.embeddingModel
        set(value) { state.embeddingModel = value }

    var maxTokens: Int
        get() = state.maxTokens
        set(value) { state.maxTokens = value }

    var temperature: Double
        get() = state.temperature
        set(value) { state.temperature = value }

    var debounceMs: Long
        get() = state.debounceMs
        set(value) { state.debounceMs = value }

    var enableRag: Boolean
        get() = state.enableRag
        set(value) { state.enableRag = value }

    companion object {
        fun getInstance(): CodexSettings {
            return ApplicationManager.getApplication().getService(CodexSettings::class.java)
        }
    }
}
