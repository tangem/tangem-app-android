plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.moneyaccount.main.impl"
}

dependencies {
    /** Common */
    implementation(projects.common.routing)

    /** Core */
    api(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.moneyAccount)
    implementation(projects.domain.wallets)

    /** Features api */
    implementation(projects.features.moneyAccount.main.api)
    implementation(projects.features.tangempay.details.api)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}