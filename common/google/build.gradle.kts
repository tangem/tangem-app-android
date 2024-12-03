plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.google"
}

dependencies {

    implementation(deps.googlePlay.services.wallet)

}