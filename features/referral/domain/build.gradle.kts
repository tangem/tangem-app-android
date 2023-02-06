plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("kapt")
}

dependencies {

    /** Libs */
    implementation(project(":core:utils"))
    implementation(project(":libs:crypto"))

    /** Time */
    implementation(Library.jodatime)

    /** DI */
    implementation(Library.hiltCore)
    kapt(Library.hiltKapt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
