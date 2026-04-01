package com.wuliner.fastgradlecn

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = MirrorToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}

class MirrorToolWindowPanel(private val project: Project) : JBPanel<MirrorToolWindowPanel>(BorderLayout()) {

    private val projectStatusLabel = JBLabel()
    private val initScriptStatusLabel = JBLabel()
    private val initScriptButton = JButton()
    private val logArea = JBTextArea(8, 40).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(12f)
    }

    init {
        border = JBUI.Borders.empty(10)

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            // Project mirrors status
            add(projectStatusLabel)
            add(Box.createVerticalStrut(4))
            val projectButtons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(JButton("Apply to Project").apply { addActionListener { onApply() } })
                add(Box.createHorizontalStrut(8))
                add(JButton("Refresh").apply { addActionListener { refreshStatus() } })
            }
            add(projectButtons)

            add(Box.createVerticalStrut(12))

            // Global init script status
            add(initScriptStatusLabel)
            add(Box.createVerticalStrut(4))
            val initButtons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(initScriptButton.apply { addActionListener { onToggleInitScript() } })
            }
            add(initButtons)
            add(Box.createVerticalStrut(8))
        }

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(logArea), BorderLayout.CENTER)

        refreshStatus()
    }

    private fun refreshStatus() {
        val applied = GradleMirrorService.checkApplied(project)
        projectStatusLabel.text = if (applied)
            "Project: ✓ CN mirrors applied"
        else
            "Project: ✗ CN mirrors not detected"

        val initInstalled = GradleInitScriptService.isInstalled()
        initScriptStatusLabel.text = if (initInstalled)
            "Global init script: ✓ Installed (auto-applies to all projects)"
        else
            "Global init script: ✗ Not installed"
        initScriptButton.text = if (initInstalled) "Remove Init Script" else "Install Init Script (Recommended)"
    }

    private fun onApply() {
        val result = GradleMirrorService.applyMirrors(project)
        logArea.text = buildLog(result)
        refreshStatus()
    }

    private fun onToggleInitScript() {
        if (GradleInitScriptService.isInstalled()) {
            val result = GradleInitScriptService.uninstall()
            logArea.text = if (result.isSuccess)
                "Global init script removed."
            else
                "Failed to remove: ${result.exceptionOrNull()?.message}"
        } else {
            val result = GradleInitScriptService.install()
            logArea.text = if (result.isSuccess)
                "✓ Global init script installed:\n${GradleInitScriptService.initFilePath()}\n\n" +
                "Aliyun mirrors will be applied automatically to every Gradle build.\n" +
                "No more manual mirror setup for new projects."
            else
                "Failed to install: ${result.exceptionOrNull()?.message}"
        }
        refreshStatus()
    }

    private fun buildLog(result: GradleMirrorService.ApplyResult): String {
        val lines = mutableListOf<String>()
        when {
            result.error != null -> lines += "Error: ${result.error}"
            result.noSettingsFile -> lines += "No settings.gradle(.kts) found in project root."
            result.alreadyApplied -> lines += "CN mirrors already applied. No changes needed."
            else -> {
                if (result.settingsModified) lines += "✓ settings.gradle(.kts): Aliyun mirrors injected"
                if (result.wrapperModified) lines += "✓ gradle-wrapper.properties: Tencent mirror + -all distribution"
                if (!result.settingsModified && !result.wrapperModified) lines += "No changes needed."
            }
        }
        if (result.error != null && result.details.isNotEmpty()) {
            lines += ""
            lines += "--- diagnostics ---"
            lines += result.details
        }
        return lines.joinToString("\n")
    }
}
