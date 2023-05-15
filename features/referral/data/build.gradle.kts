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
    implementation(project(":features:referral:domain"))
    

    /** Libs */
    implementation(project(":libs:auth"))

    /** Time */
    implementation(deps.jodatime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}