import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    alias(deps.plugins.room)
    id("configuration")
}

android {
    namespace = "com.tangem.store.datasource"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    api(deps.jodatime)
    api(deps.kotlin.coroutines)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(tangemDeps.card.core)

    api(deps.room.runtime)
    api(deps.room.ktx)
    ksp(deps.room.compiler)

    api(projects.core.remote)
    implementation(projects.core.local)
    implementation(deps.androidx.datastore.core)
    implementation(deps.androidx.datastore.preferences.core)
    implementation(projects.core.utils)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.txhistory.models)

    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
    testImplementation(deps.kotlin.coroutines)
}