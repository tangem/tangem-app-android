plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {
    implementation(project(":data:source:network"))

    implementation(deps.tangem.card.core)
}
