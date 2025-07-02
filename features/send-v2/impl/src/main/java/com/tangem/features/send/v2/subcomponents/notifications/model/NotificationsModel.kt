package com.tangem.features.send.v2.subcomponents.notifications.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedsBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addHighFeeWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addMinimumAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addRentExemptionNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addValidateTransactionNotifications
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.notifications.GetTronFeeNotificationShowCountUseCase
import com.tangem.domain.notifications.IncrementNotificationsShowCountUseCase
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeData
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.model.checkAndCalculateSubtractedAmount
import com.tangem.features.send.v2.subcomponents.fee.model.checkFeeCoverage
import com.tangem.features.send.v2.subcomponents.notifications.NotificationsUpdateTrigger
import com.tangem.features.send.v2.subcomponents.notifications.analytics.NotificationsAnalyticEvents
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class NotificationsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val appRouter: AppRouter,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val getTronFeeNotificationShowCountUseCase: GetTronFeeNotificationShowCountUseCase,
    private val incrementNotificationsShowCountUseCase: IncrementNotificationsShowCountUseCase,
    private val sendFeeReloadTrigger: SendFeeReloadTrigger,
    private val sendAmountReduceTrigger: SendAmountReduceTrigger,
    private val notificationsUpdateTrigger: NotificationsUpdateTrigger,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: SendNotificationsComponent.Params = paramsContainer.require()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val userWalletId = params.userWalletId
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus
    private val feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus
    private val currency = cryptoCurrencyStatus.currency
    private val appCurrency = params.appCurrency

    private var notificationData = params.notificationData

    private val _uiState = MutableStateFlow<ImmutableList<NotificationUM>>(persistentListOf())
    val uiState = _uiState.asStateFlow()

    private var isAmountSubtractAvailable = false

    init {
        subscribeToNotificationUpdateTrigger()
        checkIfSubtractAvailable()
        incrementNotificationsShowCount()
    }

    private fun subscribeToNotificationUpdateTrigger() {
        notificationsUpdateTrigger.updateTriggerFlow
            .onEach { updateState(it) }
            .launchIn(modelScope)
    }

    private fun checkIfSubtractAvailable() {
        modelScope.launch {
            isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(userWalletId, currency).getOrElse { false }
            buildNotifications()
        }
    }

    private fun incrementNotificationsShowCount() {
        modelScope.launch {
            incrementNotificationsShowCountUseCase(cryptoCurrencyStatus.currency)
        }
    }

    private suspend fun updateState(data: NotificationData) {
        notificationData = data
        buildNotifications()
    }

    private suspend fun buildNotifications() {
        val notifications = buildList {
            addFeeUnreachableNotification(
                tokenStatus = cryptoCurrencyStatus,
                coinStatus = feeCryptoCurrencyStatus,
                feeError = notificationData.feeError,
                onReload = {
                    modelScope.launch {
                        sendFeeReloadTrigger.triggerUpdate(
                            feeData = SendFeeData(
                                amount = notificationData.amountValue,
                                destinationAddress = notificationData.destinationAddress,
                            ),
                        )
                    }
                },
                onClick = ::showTokenDetails,
            )
            addDomainNotifications()
        }

        notificationsUpdateTrigger.callbackHasError(notifications.any { it is NotificationUM.Error })

        _uiState.value = notifications.toImmutableList()
    }

    private fun showTokenDetails(currency: CryptoCurrency) {
        appRouter.pop { isSuccess ->
            if (isSuccess) {
                appRouter.push(
                    AppRoute.CurrencyDetails(
                        userWalletId = userWalletId,
                        currency = currency,
                    ),
                )
            }
        }
    }

    private suspend fun MutableList<NotificationUM>.addDomainNotifications() = with(notificationData) {
        val balance = cryptoCurrencyStatus.value.amount ?: return
        val feeValue = fee?.amount?.value ?: return
        val isFeeCoverage = checkFeeCoverage(
            isSubtractAvailable = isAmountSubtractAvailable,
            balance = balance,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )
        val sendingAmount = checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )
        val feeCurrencyBalanceAfterTransaction = getFeeCurrencyBalanceAfterTx(
            sendingAmount = sendingAmount,
            feeValue = feeValue,
        )
        val currencyCheck = getCurrencyCheckUseCase(
            userWalletId = userWalletId,
            currencyStatus = cryptoCurrencyStatus,
            amount = sendingAmount,
            fee = feeValue,
            recipientAddress = destinationAddress,
            feeCurrencyBalanceAfterTransaction = feeCurrencyBalanceAfterTransaction,
        )

        addErrorNotifications(
            sendingAmount = sendingAmount,
            feeValue = feeValue,
            currencyCheck = currencyCheck,
        )
        addWarningNotifications(
            destinationAddress = destinationAddress,
            memo = memo,
            enteredAmount = amountValue,
            fee = fee,
            feeValue = feeValue,
            sendingAmount = sendingAmount,
            isFeeCoverage = isFeeCoverage,
            currencyCheck = currencyCheck,
        )
        addInfoNotifications()
    }

    private fun getFeeCurrencyBalanceAfterTx(sendingAmount: BigDecimal, feeValue: BigDecimal): BigDecimal? {
        val sendingCurrencyBalance = cryptoCurrencyStatus.value as? CryptoCurrencyStatus.Loaded
        val feeCurrencyBalance = feeCryptoCurrencyStatus.value as? CryptoCurrencyStatus.Loaded
        if (feeCryptoCurrencyStatus.value !is CryptoCurrencyStatus.Loaded) return null
        return when {
            feeCryptoCurrencyStatus == cryptoCurrencyStatus -> sendingCurrencyBalance?.let {
                it.amount - sendingAmount - feeValue
            }
            else -> feeCurrencyBalance?.let { it.amount - feeValue }
        }
    }

    private suspend fun MutableList<NotificationUM>.addErrorNotifications(
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
        currencyCheck: CryptoCurrencyCheck,
    ) {
        val currencyWarning = getBalanceNotEnoughForFeeWarningUseCase(
            fee = feeValue,
            userWalletId = userWalletId,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = feeCryptoCurrencyStatus,
        ).getOrNull()

        addExceedBalanceNotification(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            isSubtractionAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        addExceedsBalanceNotification(
            cryptoCurrencyWarning = currencyWarning,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(currency.network.backendId),
            onClick = ::showTokenDetails,
            onAnalyticsEvent = {
                analyticsEventHandler.send(
                    NotificationsAnalyticEvents.NoticeNotEnoughFee(
                        categoryName = analyticsCategoryName,
                        token = cryptoCurrencyStatus.currency.symbol,
                        blockchain = cryptoCurrencyStatus.currency.network.name,
                    ),
                )
            },
        )
        if (!BlockchainUtils.isCardano(currency.network.rawId)) {
            addDustWarningNotification(
                dustValue = currencyCheck.dustValue,
                feeValue = feeValue,
                sendingAmount = sendingAmount,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addTransactionLimitErrorNotification(
            currencyCheck = currencyCheck,
            sendingAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCryptoCurrencyStatus,
            feeValue = feeValue,
            onReduceClick = { reduceTo, _ ->
                modelScope.launch {
                    sendAmountReduceTrigger.triggerReduceTo(reduceTo)
                }
            },
        )
        addReserveAmountErrorNotification(
            reserveAmount = currencyCheck.reserveAmount,
            sendingAmount = sendingAmount,
            cryptoCurrency = currency,
            isAccountFunded = currencyCheck.isAccountFunded,
        )
        addMinimumAmountErrorNotification(
            minimumSendAmount = currencyCheck.minimumSendAmount,
            sendingAmount = sendingAmount,
            cryptoCurrency = currency,
        )
    }

    private suspend fun MutableList<NotificationUM>.addWarningNotifications(
        destinationAddress: String,
        memo: String?,
        enteredAmount: BigDecimal,
        sendingAmount: BigDecimal,
        fee: Fee?,
        feeValue: BigDecimal,
        isFeeCoverage: Boolean,
        currencyCheck: CryptoCurrencyCheck,
    ) {
        val validationError = validateTransactionUseCase(
            userWalletId = userWalletId,
            amount = enteredAmount.convertToSdkAmount(currency),
            fee = fee,
            memo = memo,
            destination = destinationAddress,
            network = cryptoCurrencyStatus.currency.network,
        ).leftOrNull()

        addRentExemptionNotification(
            rentWarning = currencyCheck.rentWarning,
        )

        addExistentialWarningNotification(
            existentialDeposit = currencyCheck.existentialDeposit,
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            onReduceClick = { reduceBy, reduceByDiff, _ ->
                modelScope.launch {
                    sendAmountReduceTrigger.triggerReduceBy(
                        ReduceByData(
                            reduceAmountBy = reduceBy,
                            reduceAmountByDiff = reduceByDiff,
                        ),
                    )
                }
            },
        )
        addFeeCoverageNotification(
            isFeeCoverage = isFeeCoverage,
            enteredAmountValue = enteredAmount,
            sendingValue = sendingAmount,
            appCurrency = appCurrency,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        addValidateTransactionNotifications(
            dustValue = currencyCheck.dustValue.orZero(),
            minAdaValue = (fee as? Fee.CardanoToken)?.minAdaValue,
            validationError = validationError,
            cryptoCurrency = currency,
            onReduceClick = { reduceTo, _ ->
                modelScope.launch {
                    sendAmountReduceTrigger.triggerReduceTo(reduceTo)
                }
            },
        )
        addHighFeeWarningNotification(
            enteredAmountValue = enteredAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            ignoreAmountReduce = notificationData.isIgnoreReduce,
            onReduceClick = { reduceBy, reduceByDiff, _ ->
                modelScope.launch {
                    sendAmountReduceTrigger.triggerReduceBy(
                        ReduceByData(
                            reduceAmountBy = reduceBy,
                            reduceAmountByDiff = reduceByDiff,
                        ),
                    )
                }
            },
            onCloseClick = {
                modelScope.launch {
                    sendAmountReduceTrigger.triggerIgnoreReduce()
                }
            },
        )
    }

    private suspend fun MutableList<NotificationUM>.addInfoNotifications() {
        addTronNetworkFeesNotification()
    }

    private suspend fun MutableList<NotificationUM>.addTronNetworkFeesNotification() {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val isTronToken = cryptoCurrency is CryptoCurrency.Token &&
            isTron(cryptoCurrency.network.rawId)

        if (isTronToken && getTronFeeNotificationShowCountUseCase() <= TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT) {
            add(
                NotificationUM.Info(
                    title = resourceReference(R.string.tron_will_be_send_token_fee_title),
                    subtitle = resourceReference(R.string.tron_will_be_send_token_fee_description),
                ),
            )
        }
    }

    companion object {
        const val TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT = 3
    }
}