package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class VisaWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val getVisaTxHistoryUseCase: GetVisaTxHistoryUseCase,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntentsV2, isRefresh: Boolean): WalletContentLoader {
        return VisaWalletContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            isRefresh = isRefresh,
            stateController = stateHolder,
            getVisaCurrencyUseCase = getVisaCurrencyUseCase,
            getVisaTxHistoryUseCase = getVisaTxHistoryUseCase,
        )
    }
}
