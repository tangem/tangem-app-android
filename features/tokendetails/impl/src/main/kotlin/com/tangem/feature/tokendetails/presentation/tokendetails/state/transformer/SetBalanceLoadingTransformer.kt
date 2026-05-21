package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

internal class SetBalanceLoadingTransformer(
    private val currencyIconState: CurrencyIconState,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val prevBalance = prevState.balanceBlockUM
        return prevState.copy(
            balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
                addFundsButton = prevBalance.addFundsButton,
                swapButton = prevBalance.swapButton,
                transferButton = prevBalance.transferButton,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = currencyIconState,
            ),
        )
    }
}