package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

@Suppress("LongParameterList")
internal class SingleWalletContentLoader(
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntentsV2,
    private val isRefresh: Boolean,
    private val stateHolder: WalletStateController,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            PrimaryCurrencySubscriber(
                userWallet = userWallet,
                appCurrency = appCurrency,
                stateHolder = stateHolder,
                getPrimaryCurrencyStatusUpdatesUseCase = getPrimaryCurrencyStatusUpdatesUseCase,
                setWalletWithFundsFoundUseCase = setWalletWithFundsFoundUseCase,
                analyticsEventHandler = analyticsEventHandler,
            ),
            SingleWalletButtonsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getPrimaryCurrencyStatusUpdatesUseCase = getPrimaryCurrencyStatusUpdatesUseCase,
                getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
            ),
            SingleWalletNotificationsSubscriber(
                userWalletId = userWallet.walletId,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getSingleWalletWarningsFactory = getSingleWalletWarningsFactory,
            ),
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