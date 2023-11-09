// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 这里仍然可以使用老的 buildscript，来配置需要的插件或参数
buildscript {
//    val kotlin_version = "1.8.0"
}

plugins {
    // 项目build配置应声明为false，告诉 Gradle 不要将插件应用到当前项目
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
}

allprojects {
    extensions.extraProperties.apply {
        set("versionCode", 1)
        set("versionName", "1.0")
        set("compileSdk", 33)
        set("minSdk", 24)
        set("targetSdk", 33)
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}