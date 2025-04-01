package com.tangem.features.send.v2.subcomponents.notifications.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
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
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.model.checkAndCalculateSubtractedAmount
import com.tangem.features.send.v2.subcomponents.fee.model.checkFeeCoverage
import com.tangem.features.send.v2.subcomponents.notifications.NotificationsComponent
import com.tangem.features.send.v2.subcomponents.notifications.NotificationsUpdateTrigger
import com.tangem.features.send.v2.subcomponents.notifications.analytics.NotificationsAnalyticEvents
import com.tangem.lib.crypto.BlockchainUtils
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

@Suppress("LongParameterList")
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
    private val sendFeeReloadTrigger: SendFeeReloadTrigger,
    private val sendAmountReduceTrigger: SendAmountReduceTrigger,
    private val notificationsUpdateTrigger: NotificationsUpdateTrigger,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: NotificationsComponent.Params = paramsContainer.require()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val userWalletId = params.userWalletId
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus
    private val feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus
    private val currency = cryptoCurrencyStatus.currency
    private val appCurrency = params.appCurrency

    private var destinationAddress = params.destinationAddress
    private var amountValue = params.amountValue
    private var reduceAmountBy = params.reduceAmountBy
    private var isIgnoreReduce = params.isIgnoreReduce
    private var fee = params.fee
    private var feeError = params.feeError

    private val _uiState = MutableStateFlow<ImmutableList<NotificationUM>>(persistentListOf())
    val uiState = _uiState.asStateFlow()

    private var isAmountSubtractAvailable = false

    init {
        subscribeToNotificationUpdateTrigger()
        checkIfSubtractAvailable()
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

    private suspend fun updateState(data: NotificationData) {
        destinationAddress = data.destinationAddress
        amountValue = data.amountValue
        reduceAmountBy = data.reduceAmountBy
        isIgnoreReduce = data.isIgnoreReduce
        fee = data.fee
        feeError = data.feeError

        buildNotifications()
    }

    private suspend fun buildNotifications() {
        val notifications = buildList {
            addFeeUnreachableNotification(
                tokenStatus = cryptoCurrencyStatus,
                coinStatus = feeCryptoCurrencyStatus,
                feeError = feeError,
                onReload = {
                    modelScope.launch {
                        sendFeeReloadTrigger.triggerUpdate()
                    }
                },
                onClick = ::showTokenDetails,
            )
            addDomainNotifications(
                destinationAddress = destinationAddress,
                amountValue = amountValue,
                reduceAmountBy = reduceAmountBy,
                fee = fee,
            )
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

    private suspend fun MutableList<NotificationUM>.addDomainNotifications(
        destinationAddress: String,
        amountValue: BigDecimal,
        reduceAmountBy: BigDecimal,
        fee: Fee?,
    ) {
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
            enteredAmount = amountValue,
            fee = fee,
            feeValue = feeValue,
            sendingAmount = sendingAmount,
            isFeeCoverage = isFeeCoverage,
            currencyCheck = currencyCheck,
        )
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
        if (!BlockchainUtils.isCardano(currency.network.id.value)) {
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
            memo = null,
            destination = "",
            network = currency.network,
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
            ignoreAmountReduce = isIgnoreReduce,
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
                    buildNotifications()
                }
            },
        )
    }
}