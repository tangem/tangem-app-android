plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.txhistory"
}

dependencies {
    /** Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)

    /** Project - Other */
    implementation(projects.core.utils)

    /** Android - Other */
    implementation(deps.androidx.paging.runtime)

    api(projects.core.pagination)
}