plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokenreceive.impl"
}

dependencies {

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(deps.lifecycle.compose)
    implementation(deps.kotlin.serialization)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Common */
    implementation(projects.common.ui)

    /** Core modules */
    implementation(projects.common.routing)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.res)

    /** Domain modules */
    implementation(projects.domain.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)

    /** Feature Apis */
    implementation(projects.features.tokenRecieve.api)
}