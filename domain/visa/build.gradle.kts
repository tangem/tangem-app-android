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
    /** Domain models */
    api(projects.domain.visa.models)

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.core.error)
    api(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency.models)

    /** Security */
    implementation(deps.spongecastle.core)

    /** Libs - Other */
    implementation(deps.jodatime)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
}