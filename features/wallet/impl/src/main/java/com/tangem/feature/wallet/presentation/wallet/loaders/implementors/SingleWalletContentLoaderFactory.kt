package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class SingleWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents, isRefresh: Boolean): WalletContentLoader {
        return SingleWalletContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            isRefresh = isRefresh,
            stateHolder = stateHolder,
            getSingleCryptoCurrencyStatusUseCase = getSingleCryptoCurrencyStatusUseCase,
            getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
            getSingleWalletWarningsFactory = getSingleWalletWarningsFactory,
            setWalletWithFundsFoundUseCase = setWalletWithFundsFoundUseCase,
            txHistoryItemsCountUseCase = txHistoryItemsCountUseCase,
            txHistoryItemsUseCase = txHistoryItemsUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            analyticsEventHandler = analyticsEventHandler,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            getOnrampTransactionsUseCase = getOnrampTransactionsUseCase,
            onrampRemoveTransactionUseCase = onrampRemoveTransactionUseCase,
            shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
        )
    }
}