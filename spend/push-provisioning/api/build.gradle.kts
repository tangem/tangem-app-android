plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.spend.pushProvisioning.api"
}

dependencies {
    api(deps.kotlin.coroutines)

    testImplementation(projects.test.core)
}