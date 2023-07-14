package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.utils.converter.Converter

internal class TokenListErrorToWalletStateConverter(
    private val currentState: WalletStateHolder,
) : Converter<TokenListError, WalletStateHolder> {

    // TODO: https://tangem.atlassian.net/browse/AND-4021
    override fun convert(value: TokenListError): WalletStateHolder {
        return currentState
    }
}
