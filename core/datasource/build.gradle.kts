plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {

    /** Project */
    implementation(project(":core:utils"))
    implementation(project(":libs:auth"))

    /** Tangem libraries */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)

    /** Logging */
    implementation(deps.timber)

    /** Network */
    implementation(deps.krateSharedPref)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.okHttp)
    implementation(deps.okHttp.logging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)

    /** Time */
    implementation(deps.jodatime)

    /** Security */
    implementation(deps.spongecastle.core)
}
