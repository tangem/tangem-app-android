plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.approval.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)

    /** Common */
    implementation(projects.common.ui)

    /** Other */
    implementation(deps.kotlin.immutable.collections)

    /** Compose */
    implementation(deps.compose.runtime)
}