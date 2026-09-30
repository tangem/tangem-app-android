plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins.register("meawalletSdk") {
        id = "meawallet-sdk"
        implementationClass = "com.tangem.plugin.settings.MeaWalletSdkPlugin"
    }
}