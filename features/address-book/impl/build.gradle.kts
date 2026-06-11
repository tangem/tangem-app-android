plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.addressbook.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.addressBook.api)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.addressBook)
    implementation(projects.domain.models)

    /** Common */
    implementation(projects.common.ui)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.immutable.collections)

    /** Utils */
    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.common.test)
}