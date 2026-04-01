package com.wuliner.fastgradlecn

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.io.File

class GradleProjectListener : StartupActivity.DumbAware {

    override fun runActivity(project: Project) {
        // Run file check on background thread to avoid blocking EDT
        ApplicationManager.getApplication().executeOnPooledThread {
            val basePath = project.basePath ?: return@executeOnPooledThread
            val isGradleProject = File(basePath).listFiles()?.any {
                it.name in listOf("settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle")
            } ?: false

            if (!isGradleProject) return@executeOnPooledThread
            if (GradleMirrorService.checkApplied(project)) return@executeOnPooledThread

            // Show notification on EDT
            ApplicationManager.getApplication().invokeLater {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("FastGradleCN.Notification")
                    .createNotification(
                        "FastGradleCN",
                        "CN Gradle mirrors not detected. Apply Aliyun + Tencent mirrors for faster downloads?",
                        NotificationType.INFORMATION
                    )
                    .addAction(object : NotificationAction("Apply Mirrors") {
                        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                            notification.expire()
                            ApplyMirrorsAction.applyAndNotify(project)
                        }
                    })
                    .notify(project)
            }
        }
    }
}
