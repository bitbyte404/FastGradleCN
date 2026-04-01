package com.wuliner.fastgradlecn

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class ApplyMirrorsAction : AnAction(
    MyMessageBundle.lazyMessage("action.apply.text"),
    MyMessageBundle.lazyMessage("action.apply.description"),
    null as Icon?
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        applyAndNotify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    companion object {
        fun applyAndNotify(project: Project) {
            val msg = MyMessageBundle
            val result = GradleMirrorService.applyMirrors(project)
            val (message, type) = when {
                result.error != null ->
                    "Error: ${result.error}" to NotificationType.ERROR

                result.noSettingsFile ->
                    msg.message("log.no.settings.file") to NotificationType.WARNING

                result.alreadyApplied ->
                    msg.message("log.already.applied") to NotificationType.INFORMATION

                else -> {
                    val parts = mutableListOf<String>()
                    if (result.settingsModified) parts += msg.message("log.settings.injected")
                    if (result.wrapperModified) parts += msg.message("log.wrapper.replaced")
                    (if (parts.isEmpty()) msg.message("log.no.changes") else parts.joinToString("\n")) to
                            NotificationType.INFORMATION
                }
            }

            NotificationGroupManager.getInstance()
                .getNotificationGroup("FastGradleCN.Notification")
                .createNotification("FastGradleCN", message, type)
                .notify(project)
        }
    }
}
