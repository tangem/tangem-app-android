package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetBalancesAndLimitsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("LongParameterList")
internal class VisaWalletBalancesAndLimitsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val isRefresh: Boolean,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val clickIntents: WalletClickIntentsV2,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return flow<Any> {
            stateHolder.update(
                SetBalancesAndLimitsTransformer(
                    userWallet = userWallet,
                    maybeVisaCurrency = getVisaCurrencyUseCase(userWallet.walletId, isRefresh),
                    clickIntents = clickIntents,
                ),
            )
        }
    }
}