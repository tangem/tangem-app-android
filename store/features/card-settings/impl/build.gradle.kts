plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.store.features.cardsettings.impl"
}

dependencies {
    /* Api */
    api(projects.store.features.cardSettings.api)

    /* Project - Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.decompose.ui)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.common.routing)

    /* Project - Domain */
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.tangempay)
    implementation(projects.domain.wallets)

    /* Project - Libs */
    implementation(projects.libs.tangemSdkApi)

    /* AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /* Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Test */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.wallets.models)
}