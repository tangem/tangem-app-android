plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.foryou.impl"
}

dependencies {

    /** Features */
    implementation(projects.features.forYou.api)
    implementation(projects.features.promoBanners.api)
    implementation(projects.features.commonFeatures.api)

    /** Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.account.status)
    implementation(projects.domain.appCurrency)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)

    implementation(projects.common.ui)

    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.lifecycle.compose)
    implementation(deps.compose.material3)
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
}