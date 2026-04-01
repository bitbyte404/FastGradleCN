package com.bitbyte404.fastgradlecn

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

            silentlyFixWrapper(basePath)

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
            .replace(Regex("""distributionSha256Sum=.*(\r?\n)?"""), "")
        wrapperFile.writeText(newContent, Charsets.UTF_8)

        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wrapperFile)?.refresh(false, false)
        }
    }

    private fun showSuggestionNotification(project: Project) {
        val msg = MyMessageBundle
        NotificationGroupManager.getInstance()
            .getNotificationGroup("FastGradleCN.Notification")
            .createNotification("FastGradleCN", msg.message("notification.missing"), NotificationType.INFORMATION)
            .addAction(object : NotificationAction(msg.message("notification.action.apply")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()
                    ApplyMirrorsAction.applyAndNotify(project)
                }
            })
            .notify(project)
    }
}
