package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.error.TokensError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.utils.converter.Converter

internal class TokenErrorToWalletStateConverter(
    private val currentState: WalletStateHolder,
) : Converter<TokensError, WalletStateHolder> {

    // TODO: [REDACTED_JIRA]
    override fun convert(value: TokensError): WalletStateHolder {
        return currentState
    }
}