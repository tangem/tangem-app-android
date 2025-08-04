package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.VisaWalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents

internal class VisaWalletContentLoader(
    private val userWallet: UserWallet.Cold,
    private val clickIntents: WalletClickIntents,
    private val isRefresh: Boolean,
    private val stateController: WalletStateController,
    private val getVisaTxHistoryUseCase: GetVisaTxHistoryUseCase,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            VisaWalletSubscriber(
                userWallet = userWallet,
                stateController = stateController,
                isRefresh = isRefresh,
                getVisaCurrencyUseCase = getVisaCurrencyUseCase,
                getVisaTxHistoryUseCase = getVisaTxHistoryUseCase,
                clickIntents = clickIntents,
            ),
        )
    }
}