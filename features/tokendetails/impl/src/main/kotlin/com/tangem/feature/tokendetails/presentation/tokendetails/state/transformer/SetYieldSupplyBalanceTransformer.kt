package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

/**
 * Pushes the ticking yield supply balance (emitted every tick by [YieldSupplyGetRewardsBalanceUseCase])
 * into the redesign [TokenDetailsBalanceBlockUM.Content], so the balance increments in real time.
 */
internal class SetYieldSupplyBalanceTransformer(
    private val yieldSupplyRewardBalance: YieldSupplyRewardBalance,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val balanceBlockUM = prevState.balanceBlockUM
        if (balanceBlockUM !is TokenDetailsBalanceBlockUM.Content) return prevState

        return prevState.copy(
            balanceBlockUM = balanceBlockUM.copy(
                displayYieldSupplyFiatBalance = yieldSupplyRewardBalance.fiatBalance,
                displayYieldSupplyCryptoBalance = yieldSupplyRewardBalance.cryptoBalance,
            ),
        )
    }
}