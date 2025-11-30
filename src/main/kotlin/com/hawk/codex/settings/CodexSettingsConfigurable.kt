package com.hawk.codex.settings

import com.hawk.codex.qwen.QwenClient
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

/**
 * Codex 设置界面
 * 配置通义千问 API
 */
class CodexSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var enabledCheckbox: JBCheckBox? = null
    private var apiKeyField: JBPasswordField? = null
    private var completionModelCombo: ComboBox<String>? = null
    private var maxTokensField: JBTextField? = null
    private var temperatureField: JBTextField? = null
    private var statusLabel: JBLabel? = null

    override fun getDisplayName(): String = "Codex Completion"

    override fun createComponent(): JComponent {
        val settings = CodexSettings.getInstance()

        enabledCheckbox = JBCheckBox("启用代码补全", settings.enabled)
        apiKeyField = JBPasswordField().apply {
            text = settings.apiKey
            columns = 30
        }
        completionModelCombo = ComboBox<String>().apply {
            isEditable = false
            QwenClient.AVAILABLE_MODELS.forEach { addItem(it) }
            selectedItem = settings.completionModel
        }
        maxTokensField = JBTextField(settings.maxTokens.toString(), 10)
        temperatureField = JBTextField(settings.temperature.toString(), 10)
        statusLabel = JBLabel("请配置 DashScope API Key")

        // 测试连接按钮
        val testButton = JButton("测试连接").apply {
            addActionListener { testConnection() }
        }

        // 获取 API Key 链接
        val getApiKeyButton = JButton("获取 API Key").apply {
            addActionListener {
                try {
                    java.awt.Desktop.getDesktop().browse(
                        java.net.URI("https://bailian.console.aliyun.com/?tab=ak#/api-key")
                    )
                } catch (e: Exception) {
                    statusLabel?.text = "无法打开浏览器: ${e.message}"
                }
            }
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(testButton)
            add(getApiKeyButton)
        }

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckbox!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField!!)
            .addLabeledComponent(JBLabel("补全模型:"), completionModelCombo!!)
            .addComponent(buttonPanel)
            .addLabeledComponent(JBLabel("状态:"), statusLabel!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("最大 Token 数:"), maxTokensField!!)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureField!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JPanel(BorderLayout()).apply {
            add(panel, BorderLayout.NORTH)
        }
    }

    private fun testConnection() {
        val apiKey = String(apiKeyField?.password ?: charArrayOf())
        if (apiKey.isBlank()) {
            statusLabel?.text = "✗ 请输入 API Key"
            return
        }

        val model = completionModelCombo?.selectedItem?.toString() ?: "qwen3-coder-plus"
        statusLabel?.text = "正在验证 API Key..."

        Thread {
            try {
                val client = QwenClient(apiKey, model)
                val available = client.isAvailable()
                SwingUtilities.invokeLater {
                    if (available) {
                        statusLabel?.text = "✓ API Key 有效，连接成功"
                    } else {
                        statusLabel?.text = "✗ API Key 无效或服务不可用"
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
                String(apiKeyField?.password ?: charArrayOf()) != settings.apiKey ||
                completionModelCombo?.selectedItem?.toString() != settings.completionModel ||
                maxTokensField?.text != settings.maxTokens.toString() ||
                temperatureField?.text != settings.temperature.toString()
    }

    override fun apply() {
        val settings = CodexSettings.getInstance()
        settings.enabled = enabledCheckbox?.isSelected ?: true
        settings.apiKey = String(apiKeyField?.password ?: charArrayOf())
        settings.completionModel = completionModelCombo?.selectedItem?.toString() ?: "qwen3-coder-plus"
        settings.maxTokens = maxTokensField?.text?.toIntOrNull() ?: 512
        settings.temperature = temperatureField?.text?.toDoubleOrNull() ?: 0.1
    }

    override fun reset() {
        val settings = CodexSettings.getInstance()
        enabledCheckbox?.isSelected = settings.enabled
        apiKeyField?.text = settings.apiKey
        completionModelCombo?.selectedItem = settings.completionModel
        maxTokensField?.text = settings.maxTokens.toString()
        temperatureField?.text = settings.temperature.toString()
    }
}
