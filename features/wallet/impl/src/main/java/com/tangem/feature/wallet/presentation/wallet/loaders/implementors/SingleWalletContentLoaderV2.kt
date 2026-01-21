package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class SingleWalletContentLoaderV2 @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Cold,
    @Assisted private val isRefresh: Boolean,
    @Assisted val modelScopeDependencies: ModelScopeDependencies,
    private val clickIntents: WalletClickIntents,
    private val stateHolder: WalletStateController,
    private val getCryptoCurrencyActionsUseCaseV2: GetCryptoCurrencyActionsUseCaseV2,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val dispatchers: CoroutineDispatcherProvider,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        PrimaryCurrencySubscriberV2(
            userWallet = userWallet,
            modelScopeDependencies = modelScopeDependencies,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            stateController = stateHolder,
            analyticsEventHandler = analyticsEventHandler,
        ),
        SingleWalletButtonsSubscriberV2(
            userWallet = userWallet,
            modelScopeDependencies = modelScopeDependencies,
            stateController = stateHolder,
            clickIntents = clickIntents,
            getCryptoCurrencyActionsUseCaseV2 = getCryptoCurrencyActionsUseCaseV2,
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
            hotWalletFeatureToggles = hotWalletFeatureToggles,
        ),
        SingleWalletExpressStatusesSubscriberV2(
            userWallet = userWallet,
            modelScopeDependencies = modelScopeDependencies,
            getOnrampTransactionsUseCase = getOnrampTransactionsUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            onrampRemoveTransactionUseCase = onrampRemoveTransactionUseCase,
            stateController = stateHolder,
            clickIntents = clickIntents,
            analyticsEventHandler = analyticsEventHandler,
        ),
        TxHistorySubscriberV2(
            userWallet = userWallet,
            modelScopeDependencies = modelScopeDependencies,
            txHistoryItemsCountUseCase = txHistoryItemsCountUseCase,
            txHistoryItemsUseCase = txHistoryItemsUseCase,
            isRefresh = isRefresh,
            stateController = stateHolder,
            clickIntents = clickIntents,
        ),
        CheckWalletWithFundsSubscriber(
            userWallet = userWallet,
            modelScopeDependencies = modelScopeDependencies,
            walletWithFundsChecker = walletWithFundsChecker,
            dispatchers = dispatchers,
        ),
    )

    @AssistedFactory
    interface Factory {
        fun create(
            userWallet: UserWallet.Cold,
            isRefresh: Boolean,
            modelScopeDependencies: ModelScopeDependencies,
        ): SingleWalletContentLoaderV2
    }
}