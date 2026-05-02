plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.bitbyte404"
version = findProperty("pluginVersion") as String? ?: "1.0.1"

repositories {
    maven { setUrl("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        // CI uses a downloadable Android Studio; local dev uses the installed app
        if (System.getenv("CI") != null) {
            androidStudio("2023.1.1.28")
        } else {
            local("/Applications/Android Studio.app")
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // Android Studio Hedgehog (2023.1.1) onwards, covers all recent Android Studio versions
            sinceBuild = "231"
        }

        changeNotes = """
            <ul>
                <li>自动检测项目缺少国内镜像并提示配置</li>
                <li>向 settings.gradle(.kts) 注入阿里云镜像（pluginManagement + dependencyResolutionManagement）</li>
                <li>将 gradle-wrapper.properties 的下载地址替换为腾讯云镜像</li>
                <li>支持安装全局 init script，新建任何项目自动生效</li>
                <li>支持 Kotlin DSL 和 Groovy DSL</li>
                <li>支持中英文界面</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Disable instrumentation: incompatible with Microsoft JDK (missing Packages dir)
    instrumentCode {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
