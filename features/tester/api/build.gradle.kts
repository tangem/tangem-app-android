plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tester.api"
}

dependencies {
    api(deps.lifecycle.common.java8)
}