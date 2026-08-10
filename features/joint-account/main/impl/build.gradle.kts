plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.main.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.configToggles)

    /** Features */
    implementation(projects.features.jointAccount.main.api)

    /**
     * Compose
     *
     * The `configuration` plugin enables the Compose compiler for every `:impl` module, so the runtime has to be on
     * the class path even though this module has no Composables yet.
     */
    implementation(deps.compose.runtime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}