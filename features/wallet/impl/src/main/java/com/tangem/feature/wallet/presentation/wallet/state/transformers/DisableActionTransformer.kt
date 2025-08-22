package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber
import kotlin.reflect.KClass

/**
 * Transformer for disabling action of multi-currency wallet
 *
 * @param userWalletId   user wallet id
 * @property actionClass action class that must be disabled
 */
internal class DisableActionTransformer(
    userWalletId: UserWalletId,
    private val actionClass: KClass<out WalletManageButton>,
) : WalletStateTransformer(userWalletId = userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(buttons = prevState.buttons.updateButtons())
            }
            is WalletState.MultiCurrency.Locked -> {
                Timber.w("Impossible to disable action for locked wallet")
                prevState
            }
            is WalletState.SingleCurrency -> {
                Timber.w("Impossible to disable action for single-currency wallet")
                prevState
            }
            is WalletState.Visa -> {
                Timber.w("Impossible to disable action for VISA wallet")
                prevState
            }
        }
    }

    private fun PersistentList<WalletManageButton>.updateButtons(): PersistentList<WalletManageButton> {
        return map { action ->
            if (action::class == actionClass) {
                when (action) {
                    is WalletManageButton.Buy -> action.copy(enabled = false)
                    is WalletManageButton.Swap -> action.copy(enabled = false)
                    is WalletManageButton.Sell -> action.copy(enabled = false)
                    else -> action
                }
            } else {
                action
            }
        }
            .toPersistentList()
    }
}