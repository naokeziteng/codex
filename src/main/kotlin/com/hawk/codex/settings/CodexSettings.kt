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
        var apiKey: String = "",
        var completionModel: String = "qwen3-coder-plus",
        var maxTokens: Int = 512,
        var temperature: Double = 0.1
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var enabled: Boolean
        get() = state.enabled
        set(value) { state.enabled = value }

    var apiKey: String
        get() = state.apiKey
        set(value) { state.apiKey = value }

    var completionModel: String
        get() = state.completionModel
        set(value) { state.completionModel = value }

    var maxTokens: Int
        get() = state.maxTokens
        set(value) { state.maxTokens = value }

    var temperature: Double
        get() = state.temperature
        set(value) { state.temperature = value }

    companion object {
        fun getInstance(): CodexSettings {
            return ApplicationManager.getApplication().getService(CodexSettings::class.java)
        }
    }
}
