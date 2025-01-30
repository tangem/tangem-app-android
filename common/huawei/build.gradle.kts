plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.huawei"
}

dependencies {

    implementation(deps.huawei.base)

    implementation(projects.core.ui)

    implementation(deps.timber)
}