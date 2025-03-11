package com.tangem.feature.tokendetails.presentation.tokendetails.analytics.utils

import com.tangem.domain.tokens.model.CryptoCurrency

internal fun CryptoCurrency.toAnalyticsParams(): Map<String, String> {
    return mapOf(
        "Token" to symbol,
        "Blockchain" to network.name,
    )
}