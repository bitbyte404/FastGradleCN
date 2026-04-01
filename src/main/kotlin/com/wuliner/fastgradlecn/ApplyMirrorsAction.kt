package com.wuliner.fastgradlecn

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class ApplyMirrorsAction : AnAction("Apply CN Gradle Mirrors") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        applyAndNotify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    companion object {
        fun applyAndNotify(project: Project) {
            val result = GradleMirrorService.applyMirrors(project)
            val (message, type) = when {
                result.error != null ->
                    "Error: ${result.error}" to NotificationType.ERROR

                result.noSettingsFile ->
                    "No settings.gradle(.kts) found in project root." to NotificationType.WARNING

                result.alreadyApplied ->
                    "CN mirrors already applied. No changes needed." to NotificationType.INFORMATION

                else -> {
                    val parts = mutableListOf<String>()
                    if (result.settingsModified) parts += "✓ Aliyun mirrors added to settings.gradle(.kts)"
                    if (result.wrapperModified) parts += "✓ gradle-wrapper.properties: Tencent mirror + switched to -all distribution"
                    (if (parts.isEmpty()) "No changes needed." else parts.joinToString("\n")) to
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
