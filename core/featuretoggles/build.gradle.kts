plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    defaultConfig {
        compileSdk = AppConfig.compileSdkVersion
        minSdk = AppConfig.minSdkVersion
        targetSdk = AppConfig.targetSdkVersion
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        release {
            BuildConfigFieldFactory(
                fields = listOf(Field.TesterMenuAvailability(false)),
                builder = ::buildConfigField,
            ).create()
        }

        debug {
            BuildConfigFieldFactory(
                fields = listOf(Field.TesterMenuAvailability(true)),
                builder = ::buildConfigField,
            ).create()
        }

        create("debug_beta") {
            initWith(getByName("release"))
            BuildConfigFieldFactory(
                fields = listOf(
                    Field.Environment("release"),
                    Field.TestActionEnabled(true),
                    Field.LogEnabled(true),
                    Field.TesterMenuAvailability(true),
                ),
                builder = ::buildConfigField,
            ).create()
        }
    }
}

dependencies {
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
    implementation(Library.timber)

    implementation(project(":core:datasource"))
}
