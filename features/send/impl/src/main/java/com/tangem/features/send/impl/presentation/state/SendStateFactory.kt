package com.tangem.features.send.impl.presentation.state

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.confirm.SendConfirmStateConverter
import com.tangem.features.send.impl.presentation.state.fee.SendFeeStateConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientHistoryListConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientWalletListConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

@Suppress("LongParameterList")
internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val isTapHelpPreviewEnabledProvider: Provider<Boolean>,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
) {
    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountFieldConverter(
            clickIntents = clickIntents,
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
        )
    }
    private val confirmStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendConfirmStateConverter(
            isTapHelpPreviewEnabledProvider = isTapHelpPreviewEnabledProvider,
        )
    }
    private val recipientWalletListStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientWalletListConverter()
    }
    private val recipientHistoryListStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientHistoryListConverter(
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    // region UI states
    fun getInitialState(): SendUiState = SendUiState(
        clickIntents = clickIntents,
        event = consumedEvent(),
        isEditingDisabled = false,
        isBalanceHidden = false,
        cryptoCurrencyName = "",
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

    fun getOnHideBalanceState(isBalanceHidden: Boolean): SendUiState {
        return currentStateProvider().copy(isBalanceHidden = isBalanceHidden)
    }
    //endregion

    //region recipient
    fun onLoadedWalletsList(wallets: List<AvailableWallet?>): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            recipientState = state.recipientState?.copy(
                wallets = recipientWalletListStateConverter.convert(wallets),
            ),
        )
    }

    fun onLoadedHistoryList(txHistory: List<TxHistoryItem>): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            recipientState = state.recipientState?.copy(
                recent = recipientHistoryListStateConverter.convert(txHistory),
            ),
        )
    }

    fun onRecipientAddressValueChange(value: String, isXAddress: Boolean = false): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        return state.copy(
            recipientState = recipientState.copy(
                addressTextField = recipientState.addressTextField.copy(value = value),
                memoTextField = recipientState.memoTextField?.copy(isEnabled = !isXAddress),
            ),
        )
    }

    fun getOnRecipientAddressValidState(value: String, isValidAddress: Boolean): SendUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateWalletMemoUseCase(
            memo = recipientState.memoTextField?.value.orEmpty(),
            network = cryptoCurrencyStatus.currency.network,
        ).getOrElse {
            Timber.e("Failed to validateWalletMemoUseCase: $it")
            false
        }
        val isAddressInWallet = cryptoCurrencyStatus.value.networkAddress?.availableAddresses
            ?.any { it.value == value } ?: true

        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && !isAddressInWallet,
                isValidating = false,
                addressTextField = recipientState.addressTextField.copy(
                    error = when {
                        !isValidAddress -> resourceReference(R.string.send_recipient_address_error)
                        isAddressInWallet -> resourceReference(R.string.send_error_address_same_as_wallet)
                        else -> null
                    },
                    isError = value.isNotEmpty() && !isValidAddress || isAddressInWallet,
                ),
            ),
        )
    }

    fun getOnRecipientAddressValidationStarted(): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        return state.copy(
            recipientState = recipientState.copy(isValidating = true),
        )
    }

    fun getOnRecipientMemoValueChange(value: String): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        return state.copy(
            recipientState = recipientState.copy(
                memoTextField = recipientState.memoTextField?.copy(value = value),
            ),
        )
    }

    fun getOnRecipientMemoValidState(value: String, isValidAddress: Boolean): SendUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateWalletMemoUseCase(
            memo = value,
            network = cryptoCurrencyStatus.currency.network,
        ).getOrElse {
            Timber.e("Failed to validateWalletMemoUseCase: $it")
            false
        }
        val isAddressInWallet = cryptoCurrencyStatus.value.networkAddress?.availableAddresses
            ?.any { it.value == value } ?: true

        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && !isAddressInWallet,
                isValidating = false,
                memoTextField = recipientState.memoTextField?.copy(
                    isError = value.isNotEmpty() && !isValidMemo,
                    isEnabled = true,
                ),
            ),
        )
    }

    fun getOnXAddressMemoState(): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        return state.copy(
            recipientState = recipientState.copy(
                memoTextField = recipientState.memoTextField?.copy(
                    value = "",
                    isEnabled = false,
                ),
            ),
        )
    }

    fun getHiddenRecentListState(isAddressInWallet: Boolean, isValidAddress: Boolean): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        val isNotValid = isAddressInWallet || !isValidAddress
        return state.copy(
            recipientState = recipientState.copy(
                recent = recipientState.recent.map { recent ->
                    recent.copy(isVisible = isNotValid && (recent.isLoading || recent.title != TextReference.EMPTY))
                }.toPersistentList(),
                wallets = recipientState.wallets.map { wallet ->
                    wallet.copy(isVisible = isNotValid && (wallet.isLoading || wallet.title != TextReference.EMPTY))
                }.toPersistentList(),
            ),
        )
    }
    //endregion

    //region send
    fun getSendingStateUpdate(isSending: Boolean): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            sendState = state.sendState?.copy(
                isSending = isSending,
                isPrimaryButtonEnabled = !isSending,
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
        val hasErrorNotifications = notifications.any { it is SendNotification.Error }
        return state.copy(
            sendState = sendState.copy(
                isPrimaryButtonEnabled = !hasErrorNotifications,
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
    //endregion
}