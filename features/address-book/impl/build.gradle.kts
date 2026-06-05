plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
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

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Compose */
    implementation(deps.compose.runtime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}