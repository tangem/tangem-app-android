plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency.models)

    /** Project - Other */
    implementation(projects.core.utils)

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
}