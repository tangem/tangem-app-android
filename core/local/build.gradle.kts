plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    api(deps.moshi)
    api(deps.androidx.datastore.core)
    api(deps.androidx.datastore.preferences.core)

    api(projects.core.utils)

    testImplementation(projects.test.core)
}