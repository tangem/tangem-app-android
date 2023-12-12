package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
@Suppress("LongParameterList")
internal class VisaWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateHolderV2,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun create(
        userWallet: UserWallet,
        appCurrency: AppCurrency,
        clickIntents: WalletClickIntentsV2,
        isRefresh: Boolean,
    ): WalletContentLoader {
        return VisaWalletContentLoader(
            userWallet,
            appCurrency,
            clickIntents,
            isRefresh,
            stateHolder,
            getPrimaryCurrencyStatusUpdatesUseCase,
            setWalletWithFundsFoundUseCase,
            txHistoryItemsCountUseCase,
            txHistoryItemsUseCase,
            analyticsEventHandler,
        )
    }
}
