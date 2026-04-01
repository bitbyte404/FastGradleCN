# FastGradleCN

Auto-configure Aliyun + Tencent CN mirrors for Gradle projects in Android Studio / IntelliJ IDEA.
为 Android Studio / IntelliJ IDEA 的 Gradle 项目自动配置阿里云 + 腾讯云国内镜像，解决在中国大陆下载依赖缓慢的问题。

---

## 功能 / Features

- **自动检测**：打开项目时检测是否缺少国内镜像，提示一键配置
- **注入阿里云镜像**：向 `settings.gradle(.kts)` 的 `pluginManagement.repositories` 和 `dependencyResolutionManagement.repositories` 注入阿里云仓库
- **替换 Gradle 下载地址**：将 `gradle-wrapper.properties` 的 `distributionUrl` 替换为腾讯云镜像，并自动切换为 `-all` 发行版
- **双语界面**：支持中文和英文，跟随 IDE 语言自动切换
- **兼容性强**：支持 Kotlin DSL（`.kts`）和 Groovy DSL，兼容 Android Studio Hedgehog (2023.1.1) 及以上版本

---

## 使用方式 / Usage

### 方式一：自动提示（推荐）

打开 Gradle 项目后，若未配置国内镜像，插件会自动弹出通知，点击 **Apply Mirrors** 一键配置。

### 方式二：Tool Window

侧边栏打开 **FastGradleCN** 面板，手动点击应用。

### 方式三：菜单

**Tools → 配置国内 Gradle 镜像 / Apply CN Gradle Mirrors**

---

## 镜像说明 / Mirrors

| 用途 | 镜像 |
|------|------|
| Maven 依赖 / 插件 | [阿里云 maven.aliyun.com](https://maven.aliyun.com) |
| Gradle 二进制下载 | [腾讯云 mirrors.cloud.tencent.com](https://mirrors.cloud.tencent.com/gradle/) |

---

## 安装 / Installation

**Plugin Marketplace（推荐）**

Android Studio / IntelliJ IDEA → Settings → Plugins → 搜索 `FastGradleCN`

**手动安装**

1. 前往 [Releases](../../releases) 下载最新 `.zip`
2. Settings → Plugins → ⚙️ → Install Plugin from Disk

---

## 发布 / Publish

```bash
JETBRAINS_TOKEN=your_token ./gradlew publishPlugin
```

---

## License

[Apache 2.0](LICENSE)
