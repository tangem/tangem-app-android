package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.utils.converter.Converter

internal class TokenListErrorToWalletStateConverter(
    private val currentState: WalletStateHolder,
) : Converter<TokenListError, WalletStateHolder> {
// [REDACTED_TODO_COMMENT]
    override fun convert(value: TokenListError): WalletStateHolder {
        return currentState
    }
}
