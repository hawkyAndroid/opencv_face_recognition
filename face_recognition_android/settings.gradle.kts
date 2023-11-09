// NOTE：Gradle 构建时，会先执行 settings.gradle 里面的内容（按顺序执行）
//  New Version：buildscript --> pluginManagement 插件管理
pluginManagement {
    repositories {// 下载 Gradle 插件 的依赖仓库
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

// New Version： allprojects --> dependencyResolutionManagement 依赖项解析管理
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

// include modules
include(":app")
include(":fr-sdk")

// project(':module.name') paths
project(":fr-sdk").projectDir = File(settingsDir, "../face_recognition_sdk")