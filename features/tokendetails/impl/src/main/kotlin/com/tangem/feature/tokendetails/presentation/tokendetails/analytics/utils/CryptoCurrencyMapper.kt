package com.tangem.feature.tokendetails.presentation.tokendetails.analytics.utils

import com.tangem.core.analytics.models.EventValue
import com.tangem.domain.tokens.model.CryptoCurrency

internal fun CryptoCurrency.toAnalyticsParams(): Map<String, EventValue> {
    return mapOf(
        "Token" to EventValue.StringValue(symbol),
        "Blockchain" to EventValue.StringValue(network.currencySymbol),
    )
}