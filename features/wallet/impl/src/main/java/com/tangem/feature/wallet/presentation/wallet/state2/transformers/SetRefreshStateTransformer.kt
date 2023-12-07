package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state2.model.DepositButtonState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletTokensListState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate

internal class SetRefreshStateTransformer(
    userWalletId: UserWalletId,
    private val isRefreshing: Boolean,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(
                    pullToRefreshConfig = prevState.pullToRefreshConfig.toUpdatedState(isRefreshing),
                    tokensListState = prevState.tokensListState.toUpdatedState(),
                )
            }
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(
                    pullToRefreshConfig = prevState.pullToRefreshConfig.toUpdatedState(isRefreshing),
                    buttons = prevState.buttons.toUpdatedState(),
                )
            }
            is WalletState.Visa.Content -> {
                prevState.copy(
                    pullToRefreshConfig = prevState.pullToRefreshConfig.toUpdatedState(isRefreshing),
                    depositButtonState = prevState.depositButtonState.toUpdatedState(isRefreshing),
                    balancesAndLimitBlockState = prevState.balancesAndLimitBlockState.toUpdatedState(isRefreshing),
                )
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> prevState
        }
    }

    private fun WalletPullToRefreshConfig.toUpdatedState(isRefreshing: Boolean): WalletPullToRefreshConfig {
        return copy(isRefreshing = isRefreshing)
    }

    private fun WalletTokensListState.toUpdatedState(): WalletTokensListState {
        return if (this is WalletTokensListState.ContentState.Content && organizeTokensButtonConfig != null) {
            copy(
                organizeTokensButtonConfig = organizeTokensButtonConfig.copy(
                    isEnabled = !isRefreshing,
                ),
            )
        } else {
            this
        }
    }

    private fun PersistentList<WalletManageButton>.toUpdatedState(): PersistentList<WalletManageButton> {
        val isButtonsEnabled = !isRefreshing

        return mutate {
            it.mapNotNull { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Send -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Sell -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Receive -> button
                    is WalletManageButton.Swap -> null
                }
            }
        }
    }

    private fun DepositButtonState.toUpdatedState(isRefreshing: Boolean): DepositButtonState {
        return copy(isEnabled = !isRefreshing)
    }

    private fun BalancesAndLimitsBlockState.toUpdatedState(isRefreshing: Boolean): BalancesAndLimitsBlockState {
        return when (this) {
            is BalancesAndLimitsBlockState.Content -> copy(isEnabled = !isRefreshing)
            is BalancesAndLimitsBlockState.Error,
            is BalancesAndLimitsBlockState.Loading,
            -> this
        }
    }
}