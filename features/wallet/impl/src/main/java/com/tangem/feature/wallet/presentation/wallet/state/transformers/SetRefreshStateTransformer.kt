package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.enableButtons
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
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

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> walletUM.copy(
                pullToRefreshConfig = walletUM.pullToRefreshConfig.toUpdatedState(isRefreshing),
                tokensListUM = walletUM.tokensListUM.toUpdatedState(),
                buttons = walletUM.enableButtons(),
            )
            is WalletUM.Locked -> walletUM
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

    private fun WalletTokensListUM.toUpdatedState(): WalletTokensListUM {
        return if (this is WalletTokensListUM.Content && organizeButtonUM != null) {
            copy(
                organizeButtonUM = organizeButtonUM.copy(isEnabled = !isRefreshing),
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