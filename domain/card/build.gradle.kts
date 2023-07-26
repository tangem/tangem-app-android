plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.card"
}

dependencies {
    implementation(projects.core.analytics.models)

    implementation(projects.domain.demo)
    implementation(projects.domain.core)
    implementation(projects.domain.legacy)
// [REDACTED_TODO_COMMENT]
    implementation(projects.domain.models)


    implementation(deps.tangem.card.core)
}
