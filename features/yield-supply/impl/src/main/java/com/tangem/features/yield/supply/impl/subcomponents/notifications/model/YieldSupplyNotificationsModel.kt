package com.tangem.features.yield.supply.impl.subcomponents.notifications.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedsBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addHighFeeNotificationIfNoOther
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateListener
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyNotificationsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val yieldSupplyNotificationsUpdateListener: YieldSupplyNotificationsUpdateListener,
) : Model() {

    private val params: YieldSupplyNotificationsComponent.Params = paramsContainer.require()

    val uiState: StateFlow<ImmutableList<NotificationUM>>
        field = MutableStateFlow(persistentListOf())

    init {
        subscribeToNotificationTrigger()
    }

    private fun subscribeToNotificationTrigger() {
        yieldSupplyNotificationsUpdateListener.updateTriggerFlow
            .onEach { data ->
                val cryptoCurrencyStatus = params.cryptoCurrencyStatusFlow.value
                val feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatusFlow.value

                val notifications = buildList {
                    val cryptoCurrencyWarning = getBalanceNotEnoughForFeeWarningUseCase(
                        fee = data.feeValue.orZero(),
                        userWalletId = params.userWalletId,
                        tokenStatus = cryptoCurrencyStatus,
                        feeStatus = feeCryptoCurrencyStatus,
                    ).getOrNull()

                    addExceedsBalanceNotification(
                        cryptoCurrencyWarning = cryptoCurrencyWarning,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(
                            blockchainId = cryptoCurrencyStatus.currency.network.backendId,
                        ),
                        onClick = ::openTokenDetails,
                        onAnalyticsEvent = { /*no-op*/ },
                        onResetAnalyticsEvent = { /*no-op*/ },
                    )

                    addFeeUnreachableNotification(
                        tokenStatus = cryptoCurrencyStatus,
                        coinStatus = feeCryptoCurrencyStatus,
                        feeError = data.feeError,
                        onClick = ::openTokenDetails,
                        dustValue = null,
                        onReload = params.callback::onFeeReload,
                    )

                    addHighFeeNotificationIfNoOther(
                        shouldShowHighFeeNotification = data.shouldShowHighFeeNotification,
                    )
                }

                sendAnalytics(notifications, cryptoCurrencyStatus.currency)

                uiState.update { notifications.toPersistentList() }

                // Business requirement that YieldSupplyHighNetworkFee is not an error and doesn't block the button
                val hasError = notifications.any { it !is NotificationUM.Info.YieldSupplyHighNetworkFee }
                yieldSupplyNotificationsUpdateListener.callbackHasError(hasError)
            }.launchIn(modelScope)
    }

    private fun sendAnalytics(notifications: List<NotificationUM>, currency: CryptoCurrency) {
        if (notifications.any { it is NotificationUM.Error.TokenExceedsBalance }) {
            analyticsEventHandler.send(
                YieldSupplyAnalytics.NoticeNotEnoughFee(
                    token = currency.symbol,
                    blockchain = currency.network.name,
                ),
            )
        }

        if (notifications.any { it is NotificationUM.Info.YieldSupplyHighNetworkFee }) {
            analyticsEventHandler.send(
                YieldSupplyAnalytics.NoticeHighFee(
                    token = currency.symbol,
                    blockchain = currency.network.name,
                ),
            )
        }
    }

    private fun openTokenDetails(cryptoCurrency: CryptoCurrency) {
        appRouter.push(
            AppRoute.CurrencyDetails(
                userWalletId = params.userWalletId,
                currency = cryptoCurrency,
            ),
        )
    }
}