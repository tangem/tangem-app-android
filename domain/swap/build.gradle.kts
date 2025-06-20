import org.gradle.kotlin.dsl.projects

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.swap"
}


dependencies {

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.swap.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)

    /** Util */
    implementation(projects.core.utils)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

}