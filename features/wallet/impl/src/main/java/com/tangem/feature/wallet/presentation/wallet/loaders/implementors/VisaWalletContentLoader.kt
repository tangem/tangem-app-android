package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.PrimaryCurrencySubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.TxHistorySubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.VisaWalletBalancesAndLimitsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

@Suppress("LongParameterList")
internal class VisaWalletContentLoader(
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntentsV2,
    private val isRefresh: Boolean,
    private val stateHolder: WalletStateController,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            PrimaryCurrencySubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                getPrimaryCurrencyStatusUpdatesUseCase = getPrimaryCurrencyStatusUpdatesUseCase,
                setWalletWithFundsFoundUseCase = setWalletWithFundsFoundUseCase,
                analyticsEventHandler = analyticsEventHandler,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            ),
            VisaWalletBalancesAndLimitsSubscriber(userWallet, stateHolder, clickIntents),
            TxHistorySubscriber(
                userWallet = userWallet,
                isRefresh = isRefresh,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getPrimaryCurrencyStatusUpdatesUseCase = getPrimaryCurrencyStatusUpdatesUseCase,
                txHistoryItemsCountUseCase = txHistoryItemsCountUseCase,
                txHistoryItemsUseCase = txHistoryItemsUseCase,
            ),
        )
    }
}