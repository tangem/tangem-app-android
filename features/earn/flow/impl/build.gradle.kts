plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.flow.impl"
}

dependencies {
    /** Feature */
    implementation(projects.features.earn.api)

    /** Common */
    implementation(projects.common.ui)

    /** Core */
    implementation(projects.core.ui)

    /** Kotlin */
    implementation(deps.kotlin.immutable.collections)
}