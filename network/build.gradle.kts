plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = Config.Java.sourceCompatibility
    targetCompatibility = Config.Java.targetCompatibility
}

dependencies {
    implementation(project(":common"))

    implementation(platform(Libs.Network.Okhttp3.bom))
    implementation(Libs.Network.Okhttp3.okhttp)
    implementation(Libs.Network.Okhttp3.loggingInterceptor)
    implementation(Libs.Network.Retrofit.retrofit)
    implementation(Libs.Network.Retrofit.converterMoshi)
    implementation(Libs.Network.JsonConverter.moshi)
    implementation(Libs.Network.JsonConverter.moshiKotlin)
    implementation(Libs.Network.JsonConverter.kotson)

    // Tangem
    implementation(Libs.Tangem.cardCoreSdk)

    // Kotlin
    implementation(Libs.Kotlin.coroutines)

    // Logs
    implementation(Libs.Logs.timber)
}