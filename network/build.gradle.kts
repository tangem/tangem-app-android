plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":common"))

    /** Tangem libraries */
    implementation(Tangem.cardCore)

    /** Other libraries */
    implementation(Library.retrofit)
    implementation(Library.retrofitMoshiConverter)
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.okHttp)
    implementation(Library.okHttpLogging)
    implementation(Library.timber)
    implementation(Library.coroutine)
}