plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.feedback"
}

dependencies {
    implementation(deps.arrow.core)
    implementation(deps.jodatime)

    implementation(projects.core.res)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.feedback.models)
}