package com.tangem.data.visa.utils

import com.tangem.domain.appcurrency.model.AppCurrency

internal object VisaConstants {

    const val NETWORK_NAME = "Polygon PoS"

    const val TOKEN_ID = "tether"

    val fiatCurrency = AppCurrency(
        code = "EUR",
        name = "Euro",
        symbol = "€",
    )

    /*
    * Must be `false` in production
    * Don't forget to change CardTypesResolver.isVisaWallet
    * */
    const val IS_DEMO_MODE_ENABLED = false

    const val USE_TEST_ENV = true

    const val DEMO_TESTNET_ADDRESS = "0x51d034eb1563d0d2e66379ef37756d3c14936c44"
    const val DEMO_TESTNET_PUBLIC_KEY = "03FA1122B809079F79C4E0F657FE11337FEC88C3FB3C6341B2CE2E4F5D9241DD86"

    const val DEMO_MAINNET_ADDRESS = "0x927e3ef2b3d85bacf9e520379f64f6627d323fcd"
    const val DEMO_MAINNET_PUBLIC_KEY = "02AC61CD57B8011BEE8BB489FB744845CC113AD379132C56015EE70528B6A88E92"
}

internal fun getDemoAddress(): String {
    return if (VisaConstants.USE_TEST_ENV) {
        VisaConstants.DEMO_TESTNET_ADDRESS
    } else {
        VisaConstants.DEMO_MAINNET_ADDRESS
    }
}

internal fun getDemoPublicKey(): String {
    return if (VisaConstants.USE_TEST_ENV) {
        VisaConstants.DEMO_TESTNET_PUBLIC_KEY
    } else VisaConstants.DEMO_MAINNET_PUBLIC_KEY
}
