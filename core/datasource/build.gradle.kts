plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {

    /** Project */
    implementation(project(":core:utils"))
    implementation(project(":libs:auth"))

    /** Tangem libraries */
    implementation(Tangem.blockchain)
    implementation(Tangem.cardCore)

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)

    /** Coroutines */
    implementation(Library.coroutine)

    /** Logging */
    implementation(Library.timber)

    /** Network */
    implementation(Library.krateSharedPref)
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.okHttp)
    implementation(Library.okHttpLogging)
    implementation(Library.retrofit)
    implementation(Library.retrofitMoshiConverter)

    /** Time */
    implementation(Library.jodatime)

    /** Security */
    implementation(Library.spongecastleCryptoCore)
}