package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.TransactionData
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.common.SendSyncEditConverter
import com.tangem.features.send.impl.presentation.state.confirm.SendConfirmStateConverter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.SendFeeStateConverter
import com.tangem.features.send.impl.presentation.state.fee.checkFeeCoverage
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
    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    private val amountStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountStateConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            iconStateConverter = iconStateConverter,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            maxEnterAmount = maxEnterAmountConverter.convert(cryptoCurrencyStatusProvider()),
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
        amountState = AmountState.Empty(false),
        editAmountState = AmountState.Empty(false),
    )

    fun getReadyState(): SendUiState {
        val state = currentStateProvider()
        val amountState = if (state.amountState is AmountState.Empty) {
            amountStateConverter.convert(
                AmountParameters(
                    title = stringReference(userWalletProvider().name),
                    value = "",
                ),
            )
        } else {
            state.amountState
        }
        return state.copy(
            amountState = amountState,
            recipientState = state.recipientState
                ?: recipientStateConverter.convert(SendRecipientStateConverter.Data("", null)),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
            sendState = confirmStateConverter.convert(Unit),
            cryptoCurrencyName = cryptoCurrencyStatusProvider().currency.name,
        )
    }

    fun getReadyState(amount: String, destinationAddress: String, memo: String?): SendUiState {
        val state = currentStateProvider()
        val amountState = if (state.amountState is AmountState.Empty) {
            amountStateConverter.convert(
                AmountParameters(
                    title = stringReference(userWalletProvider().name),
                    value = amount,
                ),
            )
        } else {
            state.amountState
        }
        return state.copy(
            amountState = amountState,
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
        val amountState = state.getAmountState(stateRouterProvider().isEditState) as? AmountState.Data ?: return state
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

    fun getTransactionSendState(txData: TransactionData.Uncompiled, txUrl: String): SendUiState {
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

    fun getSendNotificationState(notifications: ImmutableList<NotificationUM>): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        val reducedBy = sendState.reduceAmountBy.takeIf {
            notifications.none {
                it is NotificationUM.Error.ExistentialDeposit ||
                    it is NotificationUM.Error.TransactionLimitError ||
                    it is NotificationUM.Warning.HighFeeError
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
        notifications: ImmutableList<NotificationUM>,
    ): Boolean {
        val feeState = state.getFeeState(stateRouterProvider().isEditState) ?: return false
        val hasErrorNotifications = notifications.any { it is NotificationUM.Error }
        return !hasErrorNotifications && !isSending && feeState.feeSelectorState is FeeSelectorState.Content
    }
    //endregion
}