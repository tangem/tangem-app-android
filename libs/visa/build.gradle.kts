plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /** Project */
    implementation(projects.core.utils)

    /** Libs - Other */
    implementation(deps.web3j.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.fx)
    implementation(deps.jodatime)
    implementation(deps.okHttp.prettyLogging)
}