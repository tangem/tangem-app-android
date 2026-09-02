import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    alias(deps.plugins.room)
    id("configuration")
}

android {
    namespace = "com.tangem.grow.datasource"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    api(deps.jodatime)
    api(deps.androidx.datastore)
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    api(deps.okHttp)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    api(deps.room.runtime)
    api(deps.room.ktx)
    ksp(deps.room.compiler)

    implementation(tangemDeps.card.core)

    implementation(projects.libs.auth)

    api(projects.core.local)
    api(projects.core.remote)
    api(projects.core.utils)
    api(projects.domain.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.txhistory.models)

    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
}