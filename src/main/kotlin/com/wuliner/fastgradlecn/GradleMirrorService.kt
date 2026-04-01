package com.wuliner.fastgradlecn

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

object GradleMirrorService {

    private const val ALIYUN_GRADLE_PLUGIN = "https://maven.aliyun.com/repository/gradle-plugin"
    private const val ALIYUN_PUBLIC = "https://maven.aliyun.com/repository/public"
    private const val ALIYUN_GOOGLE = "https://maven.aliyun.com/repository/google"
    private const val TENCENT_GRADLE = "https\\://mirrors.cloud.tencent.com/gradle/"
    private const val GRADLE_OFFICIAL = "https\\://services.gradle.org/distributions/"

    data class ApplyResult(
        val settingsModified: Boolean = false,
        val wrapperModified: Boolean = false,
        val alreadyApplied: Boolean = false,
        val noSettingsFile: Boolean = false,
        val error: String? = null,
        val details: List<String> = emptyList()
    )

    fun checkApplied(project: Project): Boolean {
        val basePath = project.basePath ?: return false

        val kts = File(basePath, "settings.gradle.kts")
        val groovy = File(basePath, "settings.gradle")
        val settingsFile = when {
            kts.exists() -> kts
            groovy.exists() -> groovy
            else -> return false
        }
        val settingsOk = settingsFile.readText().contains("maven.aliyun.com")

        val wrapper = File(basePath, "gradle/wrapper/gradle-wrapper.properties")
        val wrapperOk = !wrapper.exists() || !wrapper.readText().contains("services.gradle.org")

        return settingsOk && wrapperOk
    }

    fun applyMirrors(project: Project): ApplyResult {
        val basePath = project.basePath ?: return ApplyResult(error = "No project path")
        val details = mutableListOf<String>()
        details += "basePath: $basePath"

        val settingsKts = File(basePath, "settings.gradle.kts")
        val settingsGroovy = File(basePath, "settings.gradle")
        val wrapperProps = File(basePath, "gradle/wrapper/gradle-wrapper.properties")

        val (settingsFile, isKts) = when {
            settingsKts.exists() -> settingsKts to true
            settingsGroovy.exists() -> settingsGroovy to false
            else -> return ApplyResult(noSettingsFile = true, details = details + "No settings file found")
        }
        details += "settings: ${settingsFile.path}"

        val settingsContent = settingsFile.readText()
        val alreadyApplied = settingsContent.contains("maven.aliyun.com")
        details += "settings has aliyun: $alreadyApplied"

        var settingsModified = false
        if (!alreadyApplied) {
            val newContent = injectMirrors(settingsContent, isKts)
            if (newContent != settingsContent) {
                writeFile(settingsFile)  { newContent }
                settingsModified = true
                details += "settings: mirrors injected"
            }
        }

        details += "wrapper: ${wrapperProps.path}"
        details += "wrapper exists: ${wrapperProps.exists()}"
        val wrapperModified = if (wrapperProps.exists()) {
            val content = wrapperProps.readText()
            details += "wrapper has GRADLE_OFFICIAL: ${content.contains(GRADLE_OFFICIAL)}"
            if (content.contains(GRADLE_OFFICIAL)) {
                val newContent = content
                    .replace(GRADLE_OFFICIAL, TENCENT_GRADLE)
                    .replace(Regex("""(distributionUrl=.+)-bin\.zip"""), "$1-all.zip")
                writeFile(wrapperProps) { newContent }
                details += "wrapper: replaced"
                true
            } else {
                details += "wrapper: already uses mirror or no match"
                false
            }
        } else {
            details += "wrapper: file not found, skipped"
            false
        }

        return ApplyResult(
            settingsModified = settingsModified,
            wrapperModified = wrapperModified,
            alreadyApplied = alreadyApplied && !wrapperModified,
            details = details
        )
    }

    private fun writeFile(file: File, contentProvider: () -> String) {
        val content = contentProvider()
        file.writeText(content, Charsets.UTF_8)
        // Async VFS refresh so the IDE picks up the change
        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.refresh(false, false)
        }
    }

    private fun injectMirrors(content: String, isKts: Boolean): String {
        var result = content
        result = injectIntoSection(result, "pluginManagement", isKts)
        result = injectIntoSection(result, "dependencyResolutionManagement", isKts)
        return result
    }

    private fun injectIntoSection(content: String, section: String, isKts: Boolean): String {
        val sectionIdx = content.indexOf(section)
        if (sectionIdx == -1) return content

        val sectionBrace = content.indexOf('{', sectionIdx)
        if (sectionBrace == -1) return content

        val sectionEnd = findBlockEnd(content, sectionBrace)
        if (sectionEnd == -1) return content

        val sectionBody = content.substring(sectionBrace, sectionEnd + 1)

        val repoMatch = Regex("""(?<!\w)repositories\s*\{""").find(sectionBody) ?: return content
        val repoBraceRelIdx = repoMatch.value.lastIndexOf('{') + repoMatch.range.first
        val repoBraceAbsIdx = sectionBrace + repoBraceRelIdx

        val lineStart = content.lastIndexOf('\n', repoBraceAbsIdx) + 1
        val indent = content.substring(lineStart, repoBraceAbsIdx).takeWhile { it == ' ' || it == '\t' }
        val mirrorIndent = "$indent    "

        val mirrorBlock = buildMirrorBlock(mirrorIndent, isKts)

        return content.substring(0, repoBraceAbsIdx + 1) + "\n" + mirrorBlock +
                content.substring(repoBraceAbsIdx + 1)
    }

    private fun buildMirrorBlock(indent: String, isKts: Boolean): String {
        return if (isKts) {
            "${indent}maven { setUrl(\"$ALIYUN_GRADLE_PLUGIN\") }\n" +
            "${indent}maven { setUrl(\"$ALIYUN_PUBLIC\") }\n" +
            "${indent}maven { setUrl(\"$ALIYUN_GOOGLE\") }\n"
        } else {
            "${indent}maven { url '$ALIYUN_GRADLE_PLUGIN' }\n" +
            "${indent}maven { url '$ALIYUN_PUBLIC' }\n" +
            "${indent}maven { url '$ALIYUN_GOOGLE' }\n"
        }
    }

    private fun findBlockEnd(content: String, openBraceIdx: Int): Int {
        var depth = 0
        for (i in openBraceIdx until content.length) {
            when (content[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
