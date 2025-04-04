plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    maven("https://maven.pkg.github.com/tangem/vico") {
        credentials {
            username = System.getenv("GITHUB_USER")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.tangem.vico.core:core:2.0.0-alpha.25-tangemtest")
}