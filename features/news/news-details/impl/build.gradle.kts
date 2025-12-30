plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.news.details.impl"
}

dependencies {
    /* AndroidX */
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.decompose)
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Feature modules */
    implementation(projects.features.news.newsDetails.api)
    implementation(projects.domain.models)

    /** Other dependencies */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.arrow.core)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
