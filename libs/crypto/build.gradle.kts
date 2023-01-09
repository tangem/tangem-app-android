plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {

    /** Coroutines */
    implementation(Library.coroutine)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
