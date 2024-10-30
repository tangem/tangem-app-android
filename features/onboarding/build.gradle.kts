plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.onboarding"
}

dependencies {
    /** Core modules */
    implementation(project(":common"))
    implementation(project(":domain:models"))
    implementation(project(":core:config-toggles"))
    implementation(project(":core:datasource"))
    implementation(project(":core:analytics"))
    implementation(projects.core.analytics.models)
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":core:res"))
    implementation(project(":data:common"))

    /** Tangem libraries */
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.constraintLayout)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.lifecycle.common.java8)
    implementation(deps.lifecycle.viewModel.ktx)

    /** Compose libraries */
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.compose.shimmer)

    implementation(deps.tangem.card.core)

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}