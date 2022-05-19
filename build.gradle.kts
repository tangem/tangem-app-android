buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${PluginVersion.android}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${PluginVersion.kotlin}")
        classpath("com.google.gms:google-services:${PluginVersion.googleService}")
        classpath("com.google.firebase:firebase-crashlytics-gradle:${PluginVersion.fbCrashlytics}")
        classpath("com.github.dcendents:android-maven-gradle-plugin:${PluginVersion.dcendentsMaven}")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://nexus.tangem-tech.com/repository/maven-releases/")
        maven("https://jitpack.io")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
