package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.VisaWalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

internal class VisaWalletContentLoader(
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntentsV2,
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