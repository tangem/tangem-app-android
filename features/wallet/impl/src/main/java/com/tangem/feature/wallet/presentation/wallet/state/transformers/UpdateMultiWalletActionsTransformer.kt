package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

internal class UpdateMultiWalletActionsTransformer(
    userWalletId: UserWalletId,
    private val buyStatus: Lce<Throwable, Any>,
    private val sellStatus: Lce<Throwable, Any>,
    private val swapStatus: Lce<Throwable, Any>,
) : WalletStateTransformer(userWalletId = userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(buttons = prevState.buttons.updateButtons())
            }
            is WalletState.MultiCurrency.Locked -> {
                Timber.w("Impossible to load primary currency status for locked wallet")
                prevState
            }
            is WalletState.SingleCurrency -> {
                Timber.w("Impossible to load crypto currency actions for multi-currency wallet")
                prevState
            }
            is WalletState.Visa -> {
                Timber.w("Impossible to load crypto currency actions for VISA wallet")
                prevState
            }
        }
    }

    private fun PersistentList<WalletManageButton>.updateButtons(): PersistentList<WalletManageButton> {
        return map {
            when (it) {
                is WalletManageButton.Buy -> {
                    it.copy(
                        enabled = buyStatus.isContent(),
                        dimContent = !buyStatus.isContent(),
                    )
                }
                is WalletManageButton.Sell -> {
                    it.copy(
                        enabled = sellStatus.isContent(),
                        dimContent = !sellStatus.isContent(),
                    )
                }
                is WalletManageButton.Swap -> {
                    it.copy(
                        enabled = swapStatus.isContent(),
                        dimContent = !swapStatus.isContent(),
                    )
                }
                else -> it
            }
        }
            .toPersistentList()
    }
}