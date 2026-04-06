plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.virtualaccount"
}

dependencies {
}