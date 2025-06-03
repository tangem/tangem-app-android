plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.ksp)
    id("configuration")
}
dependencies {
    /* Other */
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
}