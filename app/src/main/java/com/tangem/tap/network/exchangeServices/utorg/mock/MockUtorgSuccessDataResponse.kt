package com.tangem.tap.network.exchangeServices.utorg.mock

import com.tangem.tap.network.exchangeServices.utorg.mock.json.getCurrencyFull

/**
* [REDACTED_AUTHOR]
 */
sealed class MockUtorgSuccessDataResponse(
    val json: String,
) {
    object GetCurrency : MockUtorgSuccessDataResponse(getCurrencyFull)
}
