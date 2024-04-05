plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.features"
}

dependencies {
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":common"))
    implementation(project(":libs:auth"))
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.demo)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)

    /** Tangem libraries */
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.reKotlin)
    implementation(deps.timber)

    /** Testing libraries */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)
}
