package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Converter
import com.tangem.common.Provider
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState

// TODO: Implement this
internal class CurrencyStatusErrorConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<CurrencyStatusError, WalletSingleCurrencyState.Content> {

    override fun convert(value: CurrencyStatusError): WalletSingleCurrencyState.Content {
        return requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content)
    }
}