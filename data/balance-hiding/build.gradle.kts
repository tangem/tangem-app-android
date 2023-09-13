plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.balancehiding"
}

dependencies {

    /** DI */
    implementation(deps.hilt.core)

    kapt(deps.hilt.kapt)

    implementation(deps.kotlin.coroutines)

    implementation(projects.core.utils)
    implementation(projects.core.datasource)

    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)


}

