package com.tangem.features.send.impl.presentation.state

import androidx.paging.PagingData
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountCurrencyConverter
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.fee.SendFeeStateConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientListConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

@Suppress("LongParameterList")
internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)
    private val amountFieldConverter by lazy {
        SendAmountFieldConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }
    private val amountFieldChangeConverter by lazy {
        SendAmountFieldChangeConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val amountCurrencyConverter by lazy {
        SendAmountCurrencyConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    private val amountStateConverter by lazy {
        SendAmountStateConverter(
            appCurrencyProvider = appCurrencyProvider,
            iconStateConverter = iconStateConverter,
            userWalletProvider = userWalletProvider,
            sendAmountFieldConverter = amountFieldConverter,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val recipientStateConverter by lazy {
        SendRecipientStateConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val feeStateConverter by lazy {
        SendFeeStateConverter(
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    private val recipientListStateConverter by lazy {
        SendRecipientListConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    // region UI states
    fun getInitialState(): SendUiState = SendUiState(
        clickIntents = clickIntents,
        currentState = MutableStateFlow(SendUiStateType.None),
        event = consumedEvent(),
        isEditingDisabled = false,
        isBalanceHidden = false,
    )

    fun getReadyState(): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            amountState = state.amountState ?: amountStateConverter.convert(""),
            recipientState = state.recipientState ?: recipientStateConverter.convert(""),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
        )
    }

    fun getReadyState(amount: String, destinationAddress: String): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            amountState = state.amountState ?: amountStateConverter.convert(amount),
            recipientState = state.recipientState ?: recipientStateConverter.convert(destinationAddress),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
            isEditingDisabled = true,
        )
    }

    fun getOnHideBalanceState(isBalanceHidden: Boolean): SendUiState {
        return currentStateProvider().copy(isBalanceHidden = isBalanceHidden)
    }
    //endregion

    //region amount state clicks
    fun getOnAmountValueChange(value: String) = amountFieldChangeConverter.convert(value)

    fun getOnCurrencyChangedState(isFiat: Boolean) = amountCurrencyConverter.convert(isFiat)
    //endregion

    //region recipient
    fun onLoadedRecipientList(
        wallets: List<AvailableWallet?>,
        txHistory: PagingData<TxHistoryItem>,
        txHistoryCount: Int,
    ) {
        recipientListStateConverter.convert(
            wallets = wallets,
            txHistory = txHistory,
            txHistoryCount = txHistoryCount,
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
    //endregion

    //region send
    fun getSendingStateUpdate(isSending: Boolean): SendUiState {
        val state = currentStateProvider()
        return state.copy(sendState = state.sendState.copy(isSending = isSending))
    }

    fun getTransactionSendState(txData: TransactionData): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency

        val txUrl = getExplorerTransactionUrlUseCase(
            txHash = txData.hash.orEmpty(),
            networkId = cryptoCurrency.network.id,
        )
        return state.copy(
            sendState = state.sendState.copy(
                transactionDate = txData.date?.timeInMillis ?: System.currentTimeMillis(),
                isSuccess = true,
                txUrl = txUrl,
                notifications = persistentListOf(),
            ),
        )
    }

    fun getSendNotificationState(notifications: ImmutableList<SendNotification>): SendUiState {
        val state = currentStateProvider()
        val hasErrorNotifications = notifications.any { it is SendNotification.Error }
        return state.copy(
            sendState = state.sendState.copy(
                isPrimaryButtonEnabled = !hasErrorNotifications,
                notifications = notifications,
            ),
        )
    }
    //endregion
}