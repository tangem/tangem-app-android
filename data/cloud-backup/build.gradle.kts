plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.cloudbackup"

    // GoogleDriveTokenProvider transitively touches org.json.JSONObject, which is only a stub in unit
    // tests; return default values instead of throwing (as :common:google does for the same reason).
    testOptions.unitTests.isReturnDefaultValues = true
}

// bcprov-jdk15to18 arrives transitively and ships the same Argon2 classes as the bcprov declared
// below, so this module was compiling against two copies while :app drops that artifact globally.
// Match :app so the classpath here is the one that actually ends up in the built app.
configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
}

dependencies {
    implementation(projects.core.utils)
    implementation(projects.core.local)
    implementation(projects.core.configToggles)

    implementation(projects.common.google)
    implementation(projects.domain.cloudBackup)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.androidx.datastore)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.datetime)
    implementation(deps.bouncycastle.bcprov)
    implementation(deps.okHttp)
    implementation(deps.retrofit)
    implementation(deps.retrofit.kotlinxSerialization)

    testImplementation(projects.test.core)
}