package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.error.TokensError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.utils.converter.Converter

internal class TokenErrorToWalletStateConverter(
    private val currentState: WalletStateHolder,
) : Converter<TokensError, WalletStateHolder> {
// [REDACTED_TODO_COMMENT]
    override fun convert(value: TokensError): WalletStateHolder {
        return currentState
    }
}
