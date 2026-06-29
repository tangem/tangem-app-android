plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.visa"

    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.fx)
    api(deps.jodatime)
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.web3j.core)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion
}