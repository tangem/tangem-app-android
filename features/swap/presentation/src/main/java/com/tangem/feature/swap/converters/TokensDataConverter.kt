package com.tangem.feature.swap.converters

import com.tangem.common.Provider
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import com.tangem.feature.swap.models.Network
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelectState
import kotlinx.collections.immutable.toImmutableList

class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
    private val isBalanceHiddenProvider: Provider<Boolean>,
) {

    fun convertWithNetwork(value: FoundTokensState, network: NetworkInfo): SwapSelectTokenStateHolder {
        return SwapSelectTokenStateHolder(
            availableTokens = value.tokensInWallet.map { tokenWithBalanceToTokenToSelect(it) }.toImmutableList(),
            unavailableTokens = value.loadedTokens.map { tokenWithBalanceToTokenToSelect(it) }.toImmutableList(),
            onSearchEntered = onSearchEntered,
            onTokenSelected = onTokenSelected,
            network = Network(network.name, network.blockchainId),
        )
    }

    private fun tokenWithBalanceToTokenToSelect(tokenWithBalance: TokenWithBalance): TokenToSelectState.TokenToSelect {
        return TokenToSelectState.TokenToSelect(
            id = tokenWithBalance.token.id,
            name = tokenWithBalance.token.name,
            symbol = tokenWithBalance.token.symbol,
            isNative = tokenWithBalance.token is Currency.NativeToken,
            // todo replace converting
            tokenIcon = TokenIconState.CoinIcon(
                url = "",
                fallbackResId = 0,
                isGrayscale = false,
                showCustomBadge = false,
            ),
            addedTokenBalanceData = TokenBalanceData(
                amount = tokenWithBalance.tokenBalanceData?.amount,
                amountEquivalent = tokenWithBalance.tokenBalanceData?.amountEquivalent,
                isBalanceHidden = isBalanceHiddenProvider.invoke(),
            ),
        )
    }
}