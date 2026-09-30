plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.mcp.impl"
}

dependencies {
    /** Project - API */
    api(projects.features.mcp.api)

    /** Project - Features */
    implementation(projects.features.commonFeatures.api)
    implementation(projects.features.pushNotifications.api)

    /** Project - Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Project - Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.mcp)
    implementation(projects.domain.models)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)

    /** Other libraries */
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** DI */
    implementation(deps.androidx.activity.compose)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}