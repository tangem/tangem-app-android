plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common"
}

dependencies {

    implementation(projects.core.utils)
    api(projects.domain.models)
    api(projects.domain.appCurrency.models)
    api(projects.domain.staking.models)
    api(projects.libs.crypto)

    // region Firebase libraries
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.messaging)
    // end


    implementation(deps.arrow.core)

    testImplementation(projects.test.core)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}