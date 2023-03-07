plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("configuration")
}

dependencies {
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":common"))
    implementation(project(":libs:auth"))

    /** Tangem libraries */
    implementation(Tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(Tangem.cardCore)
    implementation(Tangem.cardAndroid) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(Library.reKotlin)
// [REDACTED_TODO_COMMENT]
    implementation(Library.retrofit)
    implementation(Library.retrofitMoshiConverter)
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.okHttpLogging)
    implementation(Library.timber)
    implementation(Library.coroutine)

    /** Testing libraries */
    testImplementation(Test.junit)
    testImplementation(Test.truth)
    androidTestImplementation(Test.junitAndroidExt)
    androidTestImplementation(Test.espresso)
}
