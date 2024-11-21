package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.UpdateMultiWalletActionsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

/**
 * Multi-currency wallet subscriber for actions state updating
 *
 * @property userWallet       user wallet
 * @property rampStateManager ramp state manager
 * @property stateController  state controller
 *
[REDACTED_AUTHOR]
 */
internal class MultiWalletActionsSubscriber(
    private val userWallet: UserWallet,
    private val rampStateManager: RampStateManager,
    private val stateController: WalletStateController,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = rampStateManager.getBuyInitializationStatus(),
            flow2 = rampStateManager.getSellInitializationStatus(),
            flow3 = rampStateManager.getSwapInitializationStatus(userWalletId = userWallet.walletId),
            transform = ::RampStatuses,
        )
            .onEach { statuses ->
                stateController.update(
                    UpdateMultiWalletActionsTransformer(
                        userWalletId = userWallet.walletId,
                        buyStatus = statuses.buy,
                        sellStatus = statuses.sell,
                        swapStatus = statuses.swap,
                    ),
                )
            }
    }

    private data class RampStatuses(
        val buy: Lce<Throwable, Any>,
        val sell: Lce<Throwable, Any>,
        val swap: Lce<Throwable, Any>,
    )
}