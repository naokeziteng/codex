package com.hawk.codex.settings

import com.hawk.codex.ollama.OllamaClient
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

/**
 * Codex 设置界面
 */
class CodexSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var enabledCheckbox: JBCheckBox? = null
    private var ollamaUrlField: JBTextField? = null
    private var completionModelCombo: ComboBox<String>? = null
    private var maxTokensField: JBTextField? = null
    private var temperatureField: JBTextField? = null
    private var debounceField: JBTextField? = null
    private var enableRagCheckbox: JBCheckBox? = null
    private var statusLabel: JBLabel? = null

    override fun getDisplayName(): String = "Codex Completion"

    override fun createComponent(): JComponent {
        val settings = CodexSettings.getInstance()

        enabledCheckbox = JBCheckBox("启用代码补全", settings.enabled)
        ollamaUrlField = JBTextField(settings.ollamaUrl, 30)
        completionModelCombo = ComboBox<String>().apply {
            isEditable = true
            addItem(settings.completionModel)
        }
        maxTokensField = JBTextField(settings.maxTokens.toString(), 10)
        temperatureField = JBTextField(settings.temperature.toString(), 10)
        debounceField = JBTextField(settings.debounceMs.toString(), 10)
        enableRagCheckbox = JBCheckBox("启用 RAG (代码库检索增强)", settings.enableRag)
        statusLabel = JBLabel("点击测试连接检查 Ollama 状态")

        // 测试连接按钮
        val testButton = JButton("测试连接").apply {
            addActionListener { testConnection() }
        }

        // 刷新模型列表按钮
        val refreshModelsButton = JButton("刷新模型列表").apply {
            addActionListener { refreshModels() }
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(testButton)
            add(refreshModelsButton)
        }

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckbox!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("Ollama 地址:"), ollamaUrlField!!)
            .addLabeledComponent(JBLabel("补全模型:"), completionModelCombo!!)
            .addComponent(buttonPanel)
            .addLabeledComponent(JBLabel("状态:"), statusLabel!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("最大 Token 数:"), maxTokensField!!)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureField!!)
            .addLabeledComponent(JBLabel("防抖延迟 (ms):"), debounceField!!)
            .addSeparator()
            .addComponent(enableRagCheckbox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JPanel(BorderLayout()).apply {
            add(panel, BorderLayout.NORTH)
        }
    }

    private fun testConnection() {
        val url = ollamaUrlField?.text ?: return
        statusLabel?.text = "正在连接..."

        Thread {
            try {
                val client = OllamaClient(url)
                val available = client.isAvailable()
                SwingUtilities.invokeLater {
                    if (available) {
                        statusLabel?.text = "✓ Ollama 连接成功"
                    } else {
                        statusLabel?.text = "✗ 无法连接到 Ollama"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel?.text = "✗ 错误: ${e.message}"
                }
            }
        }.start()
    }

    private fun refreshModels() {
        val url = ollamaUrlField?.text ?: return
        statusLabel?.text = "正在获取模型列表..."

        Thread {
            try {
                val client = OllamaClient(url)
                val models = client.listModels()
                SwingUtilities.invokeLater {
                    completionModelCombo?.removeAllItems()
                    models.forEach { model ->
                        completionModelCombo?.addItem(model)
                    }
                    if (models.isNotEmpty()) {
                        statusLabel?.text = "✓ 找到 ${models.size} 个模型"
                    } else {
                        statusLabel?.text = "未找到模型，请先用 ollama pull 下载"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel?.text = "✗ 错误: ${e.message}"
                }
            }
        }.start()
    }

    override fun isModified(): Boolean {
        val settings = CodexSettings.getInstance()
        return enabledCheckbox?.isSelected != settings.enabled ||
                ollamaUrlField?.text != settings.ollamaUrl ||
                completionModelCombo?.selectedItem?.toString() != settings.completionModel ||
                maxTokensField?.text != settings.maxTokens.toString() ||
                temperatureField?.text != settings.temperature.toString() ||
                debounceField?.text != settings.debounceMs.toString() ||
                enableRagCheckbox?.isSelected != settings.enableRag
    }

    override fun apply() {
        val settings = CodexSettings.getInstance()
        settings.enabled = enabledCheckbox?.isSelected ?: true
        settings.ollamaUrl = ollamaUrlField?.text ?: "http://localhost:11434"
        settings.completionModel = completionModelCombo?.selectedItem?.toString() ?: "deepseek-coder:6.7b"
        settings.maxTokens = maxTokensField?.text?.toIntOrNull() ?: 256
        settings.temperature = temperatureField?.text?.toDoubleOrNull() ?: 0.2
        settings.debounceMs = debounceField?.text?.toLongOrNull() ?: 300
        settings.enableRag = enableRagCheckbox?.isSelected ?: true
    }

    override fun reset() {
        val settings = CodexSettings.getInstance()
        enabledCheckbox?.isSelected = settings.enabled
        ollamaUrlField?.text = settings.ollamaUrl
        completionModelCombo?.selectedItem = settings.completionModel
        maxTokensField?.text = settings.maxTokens.toString()
        temperatureField?.text = settings.temperature.toString()
        debounceField?.text = settings.debounceMs.toString()
        enableRagCheckbox?.isSelected = settings.enableRag
    }
}
