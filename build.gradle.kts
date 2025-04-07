pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    includeBuild("plugins/configuration")
}

val properties = java.util.Properties()
val propertiesFile = File(rootDir.absolutePath, "local.properties")
if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
    println("Authenticating user: " + properties.getProperty("gpr.user"))
} else {
    println(
        "local.properties not found, please create it next to build.gradle and set gpr.user and gpr.key (Create a GitHub package read only + non expiration token at https://github.com/settings/tokens)\n" +
            "Or set GITHUB_ACTOR and GITHUB_TOKEN environment variables"
    )
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        flatDir {
            dirs("app/libs")
        }
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal {
            content {
                includeGroupAndSubgroups("com.tangem.vico")
            }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem-developments/vico")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeGroupAndSubgroups("com.tangem.vico")
            }
        }
       
    }


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")