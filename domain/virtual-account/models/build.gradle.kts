plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.virtualaccount.models"
}

dependencies {
    api(projects.domain.models)
}