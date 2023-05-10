plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":common"))
    implementation(project(":libs:auth"))
    implementation(project(":domain:models"))

    /** Tangem libraries */
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(deps.reKotlin)
    //TODO: refactoring: remove it when all network services moved to the datasource module
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.okHttp.logging)
    implementation(deps.timber)
    implementation(deps.kotlin.coroutines)

    /** Testing libraries */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)
}