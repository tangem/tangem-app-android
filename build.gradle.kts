buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.fabric.io/public")
    }

    dependencies {
        classpath(ClasspathDependency.AndroidGradlePlugin)
        classpath(ClasspathDependency.KotlinGradlePlugin)
        classpath(ClasspathDependency.AndroidMavenGradlePlugin)
        classpath(ClasspathDependency.GoogleServices)
        classpath(ClasspathDependency.GoogleFirebaseCrashlytics)
    }
}

plugins {
    id("com.google.dagger.hilt.android") version "2.44" apply false
}

allprojects {
    repositories {
        google()
        jcenter() // unable to replace with mavenCentral() due to rekotlin and com.otaliastudios:cameraview
        mavenLocal()
        maven("https://nexus.tangem-tech.com/repository/maven-releases/")
        maven("https://jitpack.io")
        maven("https://zendesk.jfrog.io/zendesk/repo")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
