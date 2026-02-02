package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
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
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> prevState
        }
    }

    private fun PullToRefreshConfig.toUpdatedState(isRefreshing: Boolean): PullToRefreshConfig {
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
                    is WalletManageButton.Stake -> null
                    is WalletManageButton.Swap -> null
                }
            }
        }
    }
}