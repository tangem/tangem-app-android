plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.dynamicaddresses"
}

dependencies {
    // region Project - Core
    implementation(projects.core.configToggles)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    implementation(projects.domain.account)
    implementation(projects.domain.dynamicAddresses)
    implementation(projects.domain.dynamicAddresses.models)
    implementation(projects.domain.models)
    implementation(projects.domain.walletManager)
    // endregion

    // region Project - Libs
    implementation(tangemDeps.blockchain) { exclude(module = "joda-time") }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion
}