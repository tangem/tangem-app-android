import org.gradle.api.JavaVersion

object Config {

    const val packageName = "com.tangem.wallet"
    const val versionCode = 1
    const val versionName = "1.0.0.-SNAPSHOT"

    object Sdk {
        const val minSDK = 21
        const val targetSDK = 31
        const val compileSdk = 31
    }

    object Java {
        val sourceCompatibility = JavaVersion.VERSION_1_8
        val targetCompatibility = JavaVersion.VERSION_1_8
    }
}