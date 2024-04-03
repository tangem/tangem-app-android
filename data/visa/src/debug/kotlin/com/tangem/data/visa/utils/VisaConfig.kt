package com.tangem.data.visa.utils

import com.tangem.domain.appcurrency.model.AppCurrency

internal object VisaConfig {

    const val NETWORK_NAME = "Polygon PoS"

    const val TOKEN_ID = "tether"

    val fiatCurrency = AppCurrency(
        code = "EUR",
        name = "Euro",
        symbol = "€",
    )
}
