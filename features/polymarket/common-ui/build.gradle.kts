plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.polymarket.commonui"
}

dependencies {
    implementation(projects.core.ui)

    api(deps.compose.foundation)
    api(deps.kotlin.immutable.collections)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.kotlin.coroutines)

    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
}