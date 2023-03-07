plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("configuration")
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
