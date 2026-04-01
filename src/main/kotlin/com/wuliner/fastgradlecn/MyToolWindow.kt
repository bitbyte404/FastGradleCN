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

    private val msg get() = MyMessageBundle

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

            add(projectStatusLabel)
            add(Box.createVerticalStrut(4))
            val projectButtons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(JButton(msg.message("button.apply.project")).apply { addActionListener { onApply() } })
                add(Box.createHorizontalStrut(8))
                add(JButton(msg.message("button.refresh")).apply { addActionListener { refreshStatus() } })
            }
            add(projectButtons)

            add(Box.createVerticalStrut(12))

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
        projectStatusLabel.text = if (GradleMirrorService.checkApplied(project))
            msg.message("status.project.applied")
        else
            msg.message("status.project.not.applied")

        val initInstalled = GradleInitScriptService.isInstalled()
        initScriptStatusLabel.text = if (initInstalled)
            msg.message("status.init.installed")
        else
            msg.message("status.init.not.installed")
        initScriptButton.text = if (initInstalled)
            msg.message("button.init.remove")
        else
            msg.message("button.init.install")
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
                msg.message("log.init.removed")
            else
                msg.message("log.init.remove.failed", result.exceptionOrNull()?.message ?: "")
        } else {
            val result = GradleInitScriptService.install()
            logArea.text = if (result.isSuccess)
                msg.message("log.init.installed", GradleInitScriptService.initFilePath())
            else
                msg.message("log.init.install.failed", result.exceptionOrNull()?.message ?: "")
        }
        refreshStatus()
    }

    private fun buildLog(result: GradleMirrorService.ApplyResult): String {
        val lines = mutableListOf<String>()
        when {
            result.error != null -> lines += "Error: ${result.error}"
            result.noSettingsFile -> lines += msg.message("log.no.settings.file")
            result.alreadyApplied -> lines += msg.message("log.already.applied")
            else -> {
                if (result.settingsModified) lines += msg.message("log.settings.injected")
                if (result.wrapperModified) lines += msg.message("log.wrapper.replaced")
                if (!result.settingsModified && !result.wrapperModified) lines += msg.message("log.no.changes")
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
