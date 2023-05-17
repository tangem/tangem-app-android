plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(project(":domain:core"))
// [REDACTED_TODO_COMMENT]
    implementation(project(":domain:models"))

    implementation(deps.tangem.card.core)
}
