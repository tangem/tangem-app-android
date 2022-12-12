plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("kapt")
}

dependencies {

    /** Libs */
    implementation(project(":libs:crypto"))
    implementation(project(":core:utils"))

    /** DI */
    implementation(Library.hiltCore)
    kapt(Library.hiltKapt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
