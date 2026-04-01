plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.dynamicaddresses"
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.dynamicAddresses.models)

    implementation(projects.domain.models)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
}