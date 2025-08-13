package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.*

@Suppress("LongParameterList")
internal class SingleWalletContentLoader(
    private val userWallet: UserWallet.Cold,
    private val clickIntents: WalletClickIntents,
    private val isRefresh: Boolean,
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
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            PrimaryCurrencySubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                getSingleCryptoCurrencyStatusUseCase = getSingleCryptoCurrencyStatusUseCase,
                setWalletWithFundsFoundUseCase = setWalletWithFundsFoundUseCase,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
                analyticsEventHandler = analyticsEventHandler,
            ),
            SingleWalletButtonsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getSingleCryptoCurrencyStatusUseCase = getSingleCryptoCurrencyStatusUseCase,
                getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
            ),
            SingleWalletNotificationsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getSingleWalletWarningsFactory = getSingleWalletWarningsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            ),
            WalletDropDownItemsSubscriber(
                stateHolder = stateHolder,
                shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
                clickIntents = clickIntents,
            ),
            SingleWalletExpressStatusesSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
                analyticsEventHandler = analyticsEventHandler,
                getSingleCryptoCurrencyStatusUseCase = getSingleCryptoCurrencyStatusUseCase,
                getOnrampTransactionsUseCase = getOnrampTransactionsUseCase,
                onrampRemoveTransactionUseCase = onrampRemoveTransactionUseCase,
            ),
            TxHistorySubscriber(
                userWallet = userWallet,
                isRefresh = isRefresh,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getSingleCryptoCurrencyStatusUseCase = getSingleCryptoCurrencyStatusUseCase,
                txHistoryItemsCountUseCase = txHistoryItemsCountUseCase,
                txHistoryItemsUseCase = txHistoryItemsUseCase,
            ),
        )
    }
}