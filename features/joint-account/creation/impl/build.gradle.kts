plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.creation.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.jointAccount.creation.api)

    /** Common */
    implementation(projects.common.ui)

    /** Features */
    implementation(projects.features.wallet.api)

    /** Domain */
    implementation(projects.domain.account.status)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.common)
    implementation(projects.domain.core)
    implementation(projects.domain.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)

    /** Tests */
    testImplementation(projects.test.core)
}