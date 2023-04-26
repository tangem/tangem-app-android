plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }

    implementation(project(":data:store:preferences"))
}
