plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

dependencies {
    /** Libs */
    implementation(project(":libs:crypto"))
    implementation(project(":core:utils"))

    /** DI */
    implementation(Library.hiltCore)
    kapt(Library.hiltKapt)

    /** Other Libraries **/
    implementation(Library.kotlinSerialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
