plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("configuration")
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