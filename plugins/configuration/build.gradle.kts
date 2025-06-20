plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(deps.gradle.kotlin)
    implementation(deps.gradle.android)
    implementation(deps.gradle.detekt)
}

gradlePlugin {
    plugins.register("configuration") {
        id = "configuration"
        implementationClass = "com.tangem.plugin.configuration.ConfigurationPlugin"
    }
}