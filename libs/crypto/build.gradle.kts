plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {

    implementation(deps.kotlin.coroutines)
    implementation(projects.domain.tokens.models)

}
