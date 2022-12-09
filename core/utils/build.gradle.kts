plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("kapt")
}

dependencies {

    /** DI */
    implementation(Library.hiltCore)
    kapt(Library.hiltKapt)

    /** Coroutines */
    implementation(Library.coroutine)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
