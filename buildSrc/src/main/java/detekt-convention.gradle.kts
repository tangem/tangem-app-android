import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("io.gitlab.arturbosch.detekt")
}

tasks.withType<Detekt> {
    parallel = true
    ignoreFailures = false
    autoCorrect = true
    buildUponDefaultConfig = true

    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    include("**/*.kt")
    exclude("**/resources/**", "**/build/**")
    reports {
        txt {
            required.set(true)
            outputLocation.set(rootProject.file("build/reports/detekt.txt"))
        }
    }

    jvmTarget = "1.8"
}

dependencies {
    detektPlugins(Tools.composeDetektRules)
    detektPlugins(Tools.formattingDetektRules)
}
