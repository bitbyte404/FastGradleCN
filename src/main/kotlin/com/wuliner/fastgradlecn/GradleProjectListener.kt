package com.wuliner.fastgradlecn

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class GradleProjectListener : StartupActivity.DumbAware {

    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val basePath = project.basePath ?: return@executeOnPooledThread
            val isGradleProject = File(basePath).listFiles()?.any {
                it.name in listOf("settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle")
            } ?: false
            if (!isGradleProject) return@executeOnPooledThread

            // Immediately and silently fix the wrapper URL before sync starts
            silentlyFixWrapper(basePath)

            // Show notification only if settings still need CN mirrors
            if (!GradleMirrorService.checkApplied(project)) {
                ApplicationManager.getApplication().invokeLater {
                    showSuggestionNotification(project)
                }
            }
        }
    }

    private fun silentlyFixWrapper(basePath: String) {
        val wrapperFile = File(basePath, "gradle/wrapper/gradle-wrapper.properties")
        if (!wrapperFile.exists()) return
        val content = wrapperFile.readText()
        if (!content.contains("services.gradle.org")) return

        val newContent = content
            .replace("https\\://services.gradle.org/distributions/", "https\\://mirrors.cloud.tencent.com/gradle/")
            .replace(Regex("""(distributionUrl=.+)-bin\.zip"""), "$1-all.zip")
        wrapperFile.writeText(newContent, Charsets.UTF_8)

        // Refresh VFS on EDT
        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wrapperFile)?.refresh(false, false)
        }
    }

    private fun showSuggestionNotification(project: Project) {
        val needInitScript = !GradleInitScriptService.isInstalled()
        val message = if (needInitScript) {
            "CN mirrors not detected. Tip: setup global init script to auto-apply on every new project."
        } else {
            "CN mirrors not detected in this project's settings.gradle(.kts)."
        }

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("FastGradleCN.Notification")
            .createNotification("FastGradleCN", message, NotificationType.INFORMATION)
            .addAction(object : NotificationAction("Apply to This Project") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()
                    ApplyMirrorsAction.applyAndNotify(project)
                }
            })

        if (needInitScript) {
            notification.addAction(object : NotificationAction("Setup Global Init Script") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()
                    val result = GradleInitScriptService.install()
                    val msg = if (result.isSuccess)
                        "✓ Init script installed: ${GradleInitScriptService.initFilePath()}\nAll future Gradle projects will use CN mirrors automatically."
                    else
                        "Failed to install: ${result.exceptionOrNull()?.message}"
                    val type = if (result.isSuccess) NotificationType.INFORMATION else NotificationType.ERROR
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("FastGradleCN.Notification")
                        .createNotification("FastGradleCN", msg, type)
                        .notify(project)
                }
            })
        }

        notification.notify(project)
    }
}
