package com.tangem.features.send.impl.presentation.state.confirm

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.minimalAmount
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedsBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addMinimumAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addRentExemptionNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addValidateTransactionNotifications
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Suppress("LongParameterList", "LargeClass")
internal class SendNotificationFactory(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val currentStateProvider: Provider<SendUiState>,
    private val stateRouterProvider: Provider<StateRouter>,
    private val isSubtractAvailableProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: SendClickIntents,
    private val userWalletId: UserWalletId,
) {

    fun create(): Flow<ImmutableList<NotificationUM>> = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Send }
        .map {
            val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

            val state = currentStateProvider()
            val isEditState = stateRouterProvider().isEditState
            val balance = cryptoCurrencyStatus.value.amount.orZero()
            val sendState = state.sendState ?: return@map persistentListOf()
            val feeState = state.getFeeState(isEditState) ?: return@map persistentListOf()
            val amountState = state.getAmountState(isEditState) as? AmountState.Data ?: return@map persistentListOf()

            val amountValue = amountState.amountTextField.cryptoAmount.value.orZero()
            val feeValue = feeState.fee?.amount?.value.orZero()
            val reduceAmountBy = sendState.reduceAmountBy.orZero()
            val isFeeCoverage = checkFeeCoverage(
                isSubtractAvailable = isSubtractAvailableProvider(),
                balance = balance,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = reduceAmountBy,
            )
            val sendingAmount = checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isSubtractAvailableProvider(),
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = reduceAmountBy,
            )
            val feeError = (feeState.feeSelectorState as? FeeSelectorState.Error)?.error

            val recipientAddress = state.recipientState?.addressTextField?.value
            val feeCurrencyBalanceAfterTransaction = getFeeCurrencyBalanceAfterTx(
                feeCurrencyStatus = feeCryptoCurrencyStatusProvider(),
                sendingCurrencyStatus = cryptoCurrencyStatus,
                sendingAmount = sendingAmount,
                feeValue = feeValue,
            )
            val currencyCheck = getCurrencyCheckUseCase(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                amount = sendingAmount,
                fee = feeValue,
                recipientAddress = recipientAddress,
                feeCurrencyBalanceAfterTransaction = feeCurrencyBalanceAfterTransaction,
            )
            buildList {
                addErrorNotifications(
                    feeError = feeError,
                    sendingAmount = sendingAmount,
                    feeValue = feeValue,
                    currencyCheck = currencyCheck,
                )
                addWarningNotifications(
                    amountState = amountState,
                    feeState = feeState,
                    sendState = sendState,
                    sendingAmount = sendingAmount,
                    isFeeCoverage = isFeeCoverage,
                    currencyCheck = currencyCheck,
                )
            }.toImmutableList()
        }

    fun dismissNotificationState(clazz: Class<out NotificationUM>, isIgnored: Boolean = false): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        val notificationsToRemove = sendState.notifications.filterIsInstance(clazz)
        val updatedNotifications = sendState.notifications.toMutableList()
        updatedNotifications.removeAll(notificationsToRemove)
        return state.copy(
            sendState = sendState.copy(
                ignoreAmountReduce = isIgnored,
                reduceAmountBy = if (isIgnored) null else sendState.reduceAmountBy,
                notifications = updatedNotifications.toImmutableList(),
            ),
        )
    }

    private fun getFeeCurrencyBalanceAfterTx(
        feeCurrencyStatus: CryptoCurrencyStatus?,
        sendingCurrencyStatus: CryptoCurrencyStatus,
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
    ): BigDecimal? {
        val sendingCurrencyBalance = sendingCurrencyStatus.value as? CryptoCurrencyStatus.Loaded
        val feeCurrencyBalance = feeCurrencyStatus?.value as? CryptoCurrencyStatus.Loaded
        if (feeCurrencyStatus?.value !is CryptoCurrencyStatus.Loaded) return null
        return when {
            feeCurrencyStatus == sendingCurrencyStatus -> sendingCurrencyBalance?.let {
                it.amount - sendingAmount - feeValue
            }
            else -> feeCurrencyBalance?.let { it.amount - feeValue }
        }
    }

    private suspend fun MutableList<NotificationUM>.addErrorNotifications(
        feeError: GetFeeError?,
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
        currencyCheck: CryptoCurrencyCheck,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val currency = cryptoCurrencyStatusProvider().currency
        val currencyWarning = getBalanceNotEnoughForFeeWarningUseCase(
            fee = feeValue,
            userWalletId = userWalletId,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = feeCryptoCurrencyStatusProvider() ?: cryptoCurrencyStatus,
        ).getOrNull()

        addFeeUnreachableNotification(
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = feeCryptoCurrencyStatusProvider() ?: cryptoCurrencyStatus,
            feeError = feeError,
            onReload = clickIntents::feeReload,
            onClick = clickIntents::onTokenDetailsClick,
        )
        addExceedBalanceNotification(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            isSubtractionAvailable = isSubtractAvailableProvider(),
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        addExceedsBalanceNotification(
            cryptoCurrencyWarning = currencyWarning,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(currency.network.backendId),
            onClick = clickIntents::onTokenDetailsClick,
            onAnalyticsEvent = {
                analyticsEventHandler.send(
                    SendAnalyticEvents.NoticeNotEnoughFee(
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
                feeCurrencyStatus = feeCryptoCurrencyStatusProvider(),
            )
        }
        addTransactionLimitErrorNotification(
            currencyCheck = currencyCheck,
            sendingAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCryptoCurrencyStatusProvider(),
            feeValue = feeValue,
            onReduceClick = clickIntents::onAmountReduceToClick,
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
        amountState: AmountState.Data,
        feeState: SendStates.FeeState,
        sendState: SendStates.SendState,
        sendingAmount: BigDecimal,
        isFeeCoverage: Boolean,
        currencyCheck: CryptoCurrencyCheck,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val currency = cryptoCurrencyStatus.currency
        val amountValue = amountState.amountTextField.cryptoAmount.value
        val validationError = amountValue?.let {
            validateTransactionUseCase(
                userWalletId = userWalletId,
                amount = amountValue.convertToSdkAmount(cryptoCurrencyStatus.currency),
                fee = feeState.fee,
                memo = null,
                destination = "",
                network = cryptoCurrencyStatus.currency.network,
            ).leftOrNull()
        }

        addRentExemptionNotification(
            rentWarning = currencyCheck.rentWarning,
        )

        addExistentialWarningNotification(
            existentialDeposit = currencyCheck.existentialDeposit,
            feeAmount = feeState.fee?.amount?.value.orZero(),
            sendingAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            onReduceClick = clickIntents::onAmountReduceByClick,
        )
        addFeeCoverageNotification(
            isFeeCoverage = isFeeCoverage,
            amountField = amountState.amountTextField,
            sendingValue = sendingAmount,
            appCurrency = appCurrencyProvider(),
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        addValidateTransactionNotifications(
            dustValue = currencyCheck.dustValue.orZero(),
            minAdaValue = (feeState.fee as? Fee.CardanoToken)?.minAdaValue,
            validationError = validationError,
            cryptoCurrency = currency,
            onReduceClick = clickIntents::onAmountReduceToClick,
        )

        addHighFeeWarningNotification(
            amountState.amountTextField.cryptoAmount.value.orZero(),
            sendState.ignoreAmountReduce,
        )
        addTooHighNotification(feeState.feeSelectorState)
        addTooLowNotification(feeState)
    }

    private fun MutableList<NotificationUM>.addHighFeeWarningNotification(
        sendAmount: BigDecimal,
        ignoreAmountReduce: Boolean,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.id.value)
        val threshold = Blockchain.Tezos.minimalAmount()
        val isTotalBalance = sendAmount >= balance && balance > threshold
        if (!ignoreAmountReduce && isTotalBalance && isTezos) {
            add(
                NotificationUM.Warning.HighFeeError(
                    currencyName = cryptoCurrencyStatus.currency.name,
                    amount = threshold.toPlainString(),
                    onConfirmClick = {
                        clickIntents.onAmountReduceByClick(
                            reduceAmountBy = threshold,
                            reduceAmountByDiff = threshold,
                            notification = NotificationUM.Warning.HighFeeError::class.java,
                        )
                    },
                    onCloseClick = {
                        clickIntents.onNotificationCancel(NotificationUM.Warning.HighFeeError::class.java)
                    },
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTooLowNotification(feeState: SendStates.FeeState) {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
        val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return
        val minimumValue = multipleFees.minimum.amount.value ?: return
        val customAmount = feeSelectorState.customValues.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        if (feeSelectorState.selectedFee == FeeType.Custom && minimumValue > customValue) {
            add(NotificationUM.Warning.FeeTooLow)
            analyticsEventHandler.send(
                SendAnalyticEvents.NoticeTransactionDelays(
                    cryptoCurrencyStatusProvider().currency.symbol,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTooHighNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState !is FeeSelectorState.Content) return

        checkIfFeeTooHigh(feeSelectorState) { diff ->
            add(NotificationUM.Warning.TooHigh(diff))
        }
    }
}