import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    parallel = true
    ignoreFailures = false
    autoCorrect = true
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

tasks.withType<Detekt> {
    include("**/*.kt")
    exclude("**/resources/**", "**/build/**")
    reports {
        sarif {
            required.set(false)
        }
        txt {
            required.set(true)
        }
    }

    jvmTarget = "1.8"
}

dependencies {
    detektPlugins(Tools.composeDetektRules)
    detektPlugins(Tools.formattingDetektRules)
}
