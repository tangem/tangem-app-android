plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.usedesk.impl"
}

dependencies {

    /** Core */
    implementation(projects.core.ui)
    implementation(projects.core.decompose)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.common.routing)
    implementation(projects.features.usedesk.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)

    /** Usedesk */
    implementation(deps.usedesk.chat.sdk)
    implementation(deps.usedesk.chat.gui)


}