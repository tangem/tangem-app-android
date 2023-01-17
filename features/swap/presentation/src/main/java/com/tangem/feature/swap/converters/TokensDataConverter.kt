package com.tangem.feature.swap.converters

import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelect
import com.tangem.utils.converter.Converter

class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
) : Converter<FoundTokensState, SwapSelectTokenStateHolder> {

    override fun convert(value: FoundTokensState): SwapSelectTokenStateHolder {
        return SwapSelectTokenStateHolder(
            tokens = (value.tokensInWallet + value.loadedTokens).map { tokenWithBalanceToTokenToSelect(it) },
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
        )
    }

    private fun tokenWithBalanceToTokenToSelect(tokenWithBalance: TokenWithBalance): TokenToSelect {
        return TokenToSelect(
            id = tokenWithBalance.token.id,
            name = tokenWithBalance.token.name,
            symbol = tokenWithBalance.token.symbol,
            iconUrl = tokenWithBalance.token.logoUrl,
            addedTokenBalanceData = TokenBalanceData(
                amount = tokenWithBalance.tokenBalanceData?.amount,
                amountEquivalent = tokenWithBalance.tokenBalanceData?.amountEquivalent,
            ),
        )
    }
}
