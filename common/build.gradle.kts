plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = Config.Java.sourceCompatibility
    targetCompatibility = Config.Java.targetCompatibility
}