package com.tangem.feature.swap.converters

import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import com.tangem.feature.swap.models.Network
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelect

class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
) {

    fun convertWithNetwork(value: FoundTokensState, network: NetworkInfo): SwapSelectTokenStateHolder {
        return SwapSelectTokenStateHolder(
            addedTokens = value.tokensInWallet.map { tokenWithBalanceToTokenToSelect(it) },
            otherTokens = value.loadedTokens.map { tokenWithBalanceToTokenToSelect(it) },
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
            network = Network(network.name, network.blockchainId),
        )
    }

    private fun tokenWithBalanceToTokenToSelect(tokenWithBalance: TokenWithBalance): TokenToSelect {
        return TokenToSelect(
            id = tokenWithBalance.token.id,
            name = tokenWithBalance.token.name,
            symbol = tokenWithBalance.token.symbol,
            iconUrl = tokenWithBalance.token.logoUrl,
            isNative = tokenWithBalance.token is Currency.NativeToken,
            addedTokenBalanceData = TokenBalanceData(
                amount = tokenWithBalance.tokenBalanceData?.amount,
                amountEquivalent = tokenWithBalance.tokenBalanceData?.amountEquivalent,
            ),
        )
    }
}
