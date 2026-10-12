plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.collectibles.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.collectibles.api)

    /** Project - Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Project - Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.pagination)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Project - Domain */
    implementation(projects.domain.collectibles)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.collectibles.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)

    /** Kotlin */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Other */
    api(deps.compose.coil)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.haze)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.common.test)
    testImplementation(projects.core.error)
}