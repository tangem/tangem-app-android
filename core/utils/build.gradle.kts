plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("configuration")
}

dependencies {

    /** DI */
    implementation(Library.hiltCore)
    kapt(Library.hiltKapt)

    /** Coroutines */
    implementation(Library.coroutine)
}
