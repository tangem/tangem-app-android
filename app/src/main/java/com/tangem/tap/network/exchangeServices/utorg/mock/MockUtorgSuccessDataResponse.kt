package com.tangem.tap.network.exchangeServices.utorg.mock

import com.tangem.tap.network.exchangeServices.utorg.mock.json.getCurrencyFull

/**
 * Created by Anton Zhilenkov on 09.02.2023.
 */
sealed class MockUtorgSuccessDataResponse(
    val json: String,
) {
    object GetCurrency : MockUtorgSuccessDataResponse(getCurrencyFull)
}
