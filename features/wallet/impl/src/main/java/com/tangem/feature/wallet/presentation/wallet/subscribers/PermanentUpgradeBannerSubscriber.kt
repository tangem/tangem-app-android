package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.hotwallet.CheckHotWalletBalanceThresholdUseCase
import com.tangem.domain.hotwallet.IsHotWalletBalanceThresholdReachedUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.PermanentUpgradeBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetPermanentUpgradeBannerTransformer
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

/**
 * Shows the permanent upgrade banner above the balance of a mobile wallet once its total balance has reached
 * $5,000. Until then every fully loaded balance is checked against the threshold.
 */
@Suppress("LongParameterList")
internal class PermanentUpgradeBannerSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val isHotWalletBalanceThresholdReachedUseCase: IsHotWalletBalanceThresholdReachedUseCase,
    private val checkHotWalletBalanceThresholdUseCase: CheckHotWalletBalanceThresholdUseCase,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BasicWalletSubscriber() {

    private val userWalletId = userWallet.walletId

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        val isThresholdReachedFlow = isHotWalletBalanceThresholdReachedUseCase(userWalletId)

        return merge(
            checkThresholdFlow(isThresholdReachedFlow),
            bannerFlow(isThresholdReachedFlow),
            bannerShownFlow(isThresholdReachedFlow),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun checkThresholdFlow(isThresholdReachedFlow: Flow<Boolean>): Flow<*> {
        return isThresholdReachedFlow.flatMapLatest { isReached ->
            if (isReached) {
                emptyFlow()
            } else {
                getAccountStatusListFlow().onEach { accountStatusList ->
                    checkHotWalletBalanceThresholdUseCase(
                        userWalletId = userWalletId,
                        totalFiatBalance = accountStatusList.totalFiatBalance,
                        currencies = accountStatusList.flattenCurrencies(),
                    ).onLeft { TangemLogger.e("Failed to check the upgrade banner balance threshold", it) }
                }
            }
        }
    }

    private fun bannerFlow(isThresholdReachedFlow: Flow<Boolean>): Flow<*> {
        return isThresholdReachedFlow.onEach { isReached ->
            val bannerUM = PermanentUpgradeBannerUM(
                onClick = { clickIntents.onPermanentUpgradeBannerClick(userWalletId) },
            )

            stateController.update(
                SetPermanentUpgradeBannerTransformer(
                    userWalletId = userWalletId,
                    permanentUpgradeBannerUM = bannerUM.takeIf { isReached },
                ),
            )
        }
    }

    private fun bannerShownFlow(isThresholdReachedFlow: Flow<Boolean>): Flow<*> {
        return combine(
            isThresholdReachedFlow,
            stateController.selectedWalletIdFlow,
            screenLifecycleProvider.isBackgroundState,
        ) { isReached, selectedWalletId, isBackground ->
            isReached && selectedWalletId == userWalletId && !isBackground
        }
            .distinctUntilChanged()
            .filter { isShown -> isShown }
            .onEach { analyticsEventHandler.send(MainScreen.NoticePermanentUpgradeBanner(userWalletId)) }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): PermanentUpgradeBannerSubscriber
    }
}