package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.common.SendSyncEditConverter
import com.tangem.features.send.impl.presentation.state.confirm.SendConfirmStateConverter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.SendFeeStateConverter
import com.tangem.features.send.impl.presentation.state.fee.checkFeeCoverage
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val isTapHelpPreviewEnabledProvider: Provider<Boolean>,
) {
    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountFieldConverter(
            clickIntents = clickIntents,
            stateRouterProvider = stateRouterProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }
    private val amountStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountStateConverter(
            appCurrencyProvider = appCurrencyProvider,
            iconStateConverter = iconStateConverter,
            userWalletProvider = userWalletProvider,
            sendAmountFieldConverter = amountFieldConverter,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val recipientStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientStateConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val feeStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendFeeStateConverter(
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val confirmStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendConfirmStateConverter(
            isTapHelpPreviewEnabledProvider = isTapHelpPreviewEnabledProvider,
        )
    }
    private val sendSyncEditConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendSyncEditConverter(currentStateProvider = currentStateProvider)
    }
    // region UI states
    fun getInitialState(): SendUiState = SendUiState(
        clickIntents = clickIntents,
        event = consumedEvent(),
        isEditingDisabled = false,
        isBalanceHidden = false,
        cryptoCurrencyName = "",
        isSubtracted = false,
    )

    fun getReadyState(): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            amountState = state.amountState ?: amountStateConverter.convert(""),
            recipientState = state.recipientState
                ?: recipientStateConverter.convert(SendRecipientStateConverter.Data("", null)),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
            sendState = confirmStateConverter.convert(Unit),
            cryptoCurrencyName = cryptoCurrencyStatusProvider().currency.name,
        )
    }

    fun getReadyState(amount: String, destinationAddress: String, memo: String?): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            amountState = state.amountState ?: amountStateConverter.convert(amount),
            recipientState = state.recipientState
                ?: recipientStateConverter.convert(SendRecipientStateConverter.Data(destinationAddress, memo)),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
            sendState = confirmStateConverter.convert(Unit),
            isEditingDisabled = true,
            cryptoCurrencyName = cryptoCurrencyStatusProvider().currency.name,
        )
    }

    fun syncEditStates(isFromEdit: Boolean) = sendSyncEditConverter.convert(isFromEdit)

    fun getOnHideBalanceState(isBalanceHidden: Boolean): SendUiState {
        return currentStateProvider().copy(isBalanceHidden = isBalanceHidden)
    }
    //endregion

    //region send
    fun getIsAmountSubtractedState(isAmountSubtractAvailable: Boolean): SendUiState {
        val state = currentStateProvider()
        val balance = cryptoCurrencyStatusProvider().value.amount ?: return state
        val amountState = state.getAmountState(stateRouterProvider().isEditState) ?: return state
        val feeState = state.getFeeState(stateRouterProvider().isEditState) ?: return state
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: return state
        val feeValue = feeState.fee?.amount?.value ?: BigDecimal.ZERO
        return state.copy(
            isSubtracted = checkFeeCoverage(
                isSubtractAvailable = isAmountSubtractAvailable,
                balance = balance,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = state.sendState?.reduceAmountBy,
            ),
        )
    }

    fun getSendingStateUpdate(isSending: Boolean): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            sendState = state.sendState?.copy(
                isSending = isSending,
                isPrimaryButtonEnabled = isPrimaryButtonEnabled(
                    state = state,
                    isSending = isSending,
                    notifications = state.sendState.notifications,
                ),
            ),
        )
    }

    fun getTransactionSendState(txData: TransactionData, txUrl: String): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        return state.copy(
            sendState = sendState.copy(
                transactionDate = txData.date?.timeInMillis ?: System.currentTimeMillis(),
                isSuccess = true,
                showTapHelp = false,
                txUrl = txUrl,
                notifications = persistentListOf(),
            ),
        )
    }

    fun getSendNotificationState(notifications: ImmutableList<SendNotification>): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        val reducedBy = sendState.reduceAmountBy.takeIf {
            notifications.none {
                it is SendNotification.Error.ExistentialDeposit ||
                    it is SendNotification.Error.TransactionLimitError ||
                    it is SendNotification.Warning.HighFeeError
            }
        }
        return state.copy(
            sendState = sendState.copy(
                isPrimaryButtonEnabled = isPrimaryButtonEnabled(
                    state = state,
                    isSending = sendState.isSending,
                    notifications = notifications,
                ),
                reduceAmountBy = reducedBy,
                notifications = notifications,
                showTapHelp = sendState.showTapHelp && notifications.isEmpty(),
            ),
        )
    }

    fun getHiddenTapHelpState(): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        return state.copy(
            sendState = sendState.copy(showTapHelp = false),
        )
    }

    private fun isPrimaryButtonEnabled(
        state: SendUiState,
        isSending: Boolean,
        notifications: ImmutableList<SendNotification>,
    ): Boolean {
        val feeState = state.getFeeState(stateRouterProvider().isEditState) ?: return false
        val hasErrorNotifications = notifications.any { it is SendNotification.Error }
        return !hasErrorNotifications && !isSending && feeState.feeSelectorState is FeeSelectorState.Content
    }
    //endregion
}
