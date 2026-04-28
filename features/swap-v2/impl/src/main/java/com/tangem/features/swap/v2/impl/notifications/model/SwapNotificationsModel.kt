package com.tangem.features.swap.v2.impl.notifications.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.transaction.usecase.IsMemoRequiredUseCase
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.amount.entity.PriceImpact
import com.tangem.features.swap.v2.impl.notifications.DefaultSwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent.Params.SwapNotificationData
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.entity.SwapNotificationUM
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class SwapNotificationsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val swapNotificationsUpdateListener: SwapNotificationsUpdateListener,
    private val swapNotificationsUpdateTrigger: DefaultSwapNotificationsUpdateTrigger,
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger,
    private val isMemoRequiredUseCase: IsMemoRequiredUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SwapNotificationsComponent.Params = paramsContainer.require()

    private var notificationData = params.swapNotificationData
    private var lastSentErrorKeys: Set<Pair<String, Map<String, String>>> = emptySet()

    val uiState: StateFlow<ImmutableList<NotificationUM>>
        field = MutableStateFlow<ImmutableList<NotificationUM>>(persistentListOf())

    init {
        subscribeToNotificationUpdateTrigger()
        modelScope.launch {
            buildNotifications()
        }
    }

    private fun subscribeToNotificationUpdateTrigger() {
        swapNotificationsUpdateListener.updateTriggerFlow
            .onEach { updateState(it) }
            .launchIn(modelScope)
    }

    private suspend fun updateState(data: SwapNotificationData) {
        notificationData = data
        buildNotifications()
    }

    private suspend fun buildNotifications() {
        val notifications = buildList {
            addInsufficientFundsNotification()
            addExpressErrorNotification()
            addDestinationTagRequiredNotification()
            maybeAddPriceImpactNotification()
        }

        val hasErrorNotification = notifications
            .filterNot { notification ->
                notification == SwapNotificationUM.Warning.TradeTooHigh ||
                    notification == SwapNotificationUM.Warning.HighPriceImpact
            }
            .isNotEmpty()
        swapNotificationsUpdateTrigger.callbackHasError(hasErrorNotification)
        uiState.value = notifications.toImmutableList()

        val fromCurrency = notificationData.fromCryptoCurrency
        val toCurrency = notificationData.toCryptoCurrencyStatus?.currency
        val provider = notificationData.provider
        if (fromCurrency != null && toCurrency != null && provider != null) {
            if (notifications.any { it is SwapNotificationUM.Warning.HighPriceImpact }) {
                analyticsEventHandler.send(
                    SendWithSwapAnalyticEvents.HighPriceImpact(
                        sendToken = fromCurrency.symbol,
                        receiveToken = toCurrency.symbol,
                        sendBlockchain = fromCurrency.network.name,
                        receiveBlockchain = toCurrency.network.name,
                        providerName = provider.name,
                    ),
                )
            }
            if (notifications.any { it is SwapNotificationUM.Warning.TradeTooHigh }) {
                analyticsEventHandler.send(
                    SendWithSwapAnalyticEvents.TradeTooLarge(
                        sendToken = fromCurrency.symbol,
                        receiveToken = toCurrency.symbol,
                        sendBlockchain = fromCurrency.network.name,
                        receiveBlockchain = toCurrency.network.name,
                        providerName = provider.name,
                    ),
                )
            }
        }

        sendErrorAnalyticsIfNeeded(notifications)
    }

    private suspend fun MutableList<NotificationUM>.addDestinationTagRequiredNotification() {
        val toCryptoCurrencyStatus = notificationData.toCryptoCurrencyStatus ?: return
        val destinationAddress = notificationData.destinationAddress
        if (destinationAddress.isEmpty()) return

        val isMemoRequired = if (notificationData.memo.isNullOrEmpty()) {
            isMemoRequiredUseCase(
                network = toCryptoCurrencyStatus.currency.network,
                destinationAddress = destinationAddress,
            )
        } else {
            false
        }
        if (isMemoRequired) {
            add(NotificationUM.Error.DestinationTagRequired)
        }
    }

    private fun MutableList<NotificationUM>.addInsufficientFundsNotification() {
        val enteredFromAmount = notificationData.enteredFromAmount ?: return
        val balance = notificationData.fromCryptoCurrencyStatus?.value?.amount ?: return
        val totalRequired = if (notificationData.shouldIncludeFeeInBalanceCheck) {
            enteredFromAmount + (notificationData.feeValue ?: BigDecimal.ZERO)
        } else {
            enteredFromAmount
        }
        if (totalRequired > balance) {
            add(SwapNotificationUM.Error.InsufficientFunds)
        }
    }

    fun MutableList<NotificationUM>.addExpressErrorNotification() {
        val expressError = notificationData.expressError ?: return
        val fromCryptoCurrency = notificationData.fromCryptoCurrency ?: return
        val toCryptoCurrency = notificationData.toCryptoCurrencyStatus?.currency ?: return

        val amountErrorCurrency = if (notificationData.rateType == ExpressRateType.Fixed) {
            toCryptoCurrency
        } else {
            fromCryptoCurrency
        }

        val errorNotification = when (expressError) {
            is ExpressError.AmountError.TooSmallError -> SwapNotificationUM.Error.MinimalAmountError(
                expressError.amount.format {
                    crypto(
                        symbol = amountErrorCurrency.symbol,
                        decimals = amountErrorCurrency.decimals,
                    )
                },
            )
            is ExpressError.AmountError.TooBigError -> SwapNotificationUM.Error.MaximumAmountError(
                expressError.amount.format {
                    crypto(
                        symbol = amountErrorCurrency.symbol,
                        decimals = amountErrorCurrency.decimals,
                    )
                },
            )
            else -> SwapNotificationUM.Warning.ExpressGeneralError(
                expressError = expressError,
                onConfirmClick = {
                    modelScope.launch {
                        swapAmountUpdateTrigger.triggerQuoteReload()
                    }
                },
            )
        }

        add(errorNotification)
    }

    private fun MutableList<NotificationUM>.maybeAddPriceImpactNotification() {
        val priceImpact = notificationData.priceImpact ?: return
        if (!priceImpact.shouldShowWarning()) return

        val notification = when (priceImpact.type) {
            PriceImpact.Type.HIGH -> SwapNotificationUM.Warning.TradeTooHigh
            PriceImpact.Type.MEDIUM -> SwapNotificationUM.Warning.HighPriceImpact
            else -> return
        }

        add(notification)
    }

    private fun sendErrorAnalyticsIfNeeded(notifications: List<NotificationUM>) {
        val fromToken = notificationData.fromCryptoCurrency ?: return
        val toToken = notificationData.toCryptoCurrencyStatus?.currency

        val events = notifications.mapNotNull { notification ->
            when (notification) {
                is SwapNotificationUM.Error.InsufficientFunds ->
                    SendWithSwapAnalyticEvents.ErrorInsufficientBalance(fromToken = fromToken)
                is SwapNotificationUM.Error.MinimalAmountError ->
                    SendWithSwapAnalyticEvents.ErrorMinAmount(fromToken = fromToken)
                is SwapNotificationUM.Error.MaximumAmountError ->
                    SendWithSwapAnalyticEvents.ErrorMaxAmount(fromToken = fromToken)
                is SwapNotificationUM.Warning.ExpressGeneralError -> toToken?.let { receiveToken ->
                    SendWithSwapAnalyticEvents.ErrorExpressQuote(
                        fromToken = fromToken,
                        toToken = receiveToken,
                        errorDescription = "code=${notification.expressError.code}",
                    )
                }
                else -> null
            }
        }

        val currentKeys = events.map { it.event to it.params }.toSet()
        val newEvents = events.filter { it.event to it.params !in lastSentErrorKeys }
        lastSentErrorKeys = currentKeys

        newEvents.forEach(analyticsEventHandler::send)
    }
}