plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /** Coroutines */
    implementation(deps.kotlin.coroutines)
}
