plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.deeplink.global"
}

dependencies {

    /* Project */
    implementation(projects.core.deepLinks)
}