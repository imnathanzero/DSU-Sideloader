buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath(Google.dagger.hilt.android.gradlePlugin)
        classpath("org.jetbrains.kotlin:kotlin-serialization:_")
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false

    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("com.google.devtools.ksp") apply false

    id("com.mikepenz.aboutlibraries.plugin") apply false
    id("org.jmailen.kotlinter") apply false
}

extra.set("versionCode", 8)
extra.set("versionName", "2.03")
extra.set("packageName", "git.imnathanzero.sysphere")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}