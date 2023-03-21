plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {

    /** Project*/
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":features:swap:domain"))

    /** Network */
    implementation(deps.retrofit)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
