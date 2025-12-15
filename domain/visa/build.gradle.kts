plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.visa"
}

dependencies {
    /** Project - Core */
    api(projects.core.pagination)
    implementation(projects.core.utils)
    implementation(projects.core.error)
    implementation(projects.core.analytics.models)

    /** Project - Domain */
    api(projects.domain.models)
    api(projects.domain.visa.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Security */
    implementation(deps.spongecastle.core)

    /** Libs - Other */
    implementation(deps.timber)
    implementation(deps.jodatime)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
}