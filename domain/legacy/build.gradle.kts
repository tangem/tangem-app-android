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
    implementation(projects.domain.demo)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
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
    implementation(deps.reKotlin)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    implementation(deps.kotlin.coroutines)

    /** Testing libraries */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)
}
