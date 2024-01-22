package com.tangem.features.send.impl.presentation.state

import androidx.paging.PagingData
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientListConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LongParameterList", "LargeClass")
internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)
    private val amountFieldConverter by lazy { SendAmountFieldConverter(clickIntents) }
    private val amountFieldChangeConverter by lazy { SendAmountFieldChangeConverter(currentStateProvider) }
    private val customFeeFieldConverter by lazy {
        SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    private val feeNotificationFactory = FeeNotificationFactory(
        coinCryptoCurrencyStatusProvider = coinCryptoCurrencyStatusProvider,
        userWalletProvider = userWalletProvider,
        clickIntents = clickIntents,
    )

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
        currentState = MutableStateFlow(SendUiStateType.Amount),
        event = consumedEvent(),
    )

    fun getReadyState(): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            amountState = state.amountState ?: amountStateConverter.convert(Unit),
            recipientState = state.recipientState ?: recipientStateConverter.convert(Unit),
            feeState = state.feeState ?: feeStateConverter.convert(Unit),
        )
    }
    //endregion

    //region amount state clicks
    fun getOnAmountValueChange(value: String) = amountFieldChangeConverter.convert(value)

    fun getOnCurrencyChangedState(isFiat: Boolean): SendUiState {
        val state = currentStateProvider()
        val amountState = state.amountState ?: return state

        return if (amountState.isFiatValue == isFiat) {
            state
        } else {
            return state.copy(amountState = amountState.copy(isFiatValue = isFiat))
        }
    }
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

    fun onRecipientAddressValueChange(value: String): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state
        return state.copy(
            recipientState = recipientState.copy(
                addressTextField = recipientState.addressTextField.copy(value = value),
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
                ),
            ),
        )
    }
    //endregion

    //region fee
    fun onFeeOnLoadingState(): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = FeeSelectorState.Loading
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = feeSelectorState,
                notifications = persistentListOf(),
                isPrimaryButtonEnabled = feeSelectorState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onFeeOnLoadedState(fees: TransactionFee): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = FeeSelectorState.Content(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )

        val fee = feeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee, feeState.isSubtract)
        val updatedState = feeState.copy(
            feeSelectorState = feeSelectorState,
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(feeState = updatedState),
                isPrimaryButtonEnabled = feeSelectorState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onFeeSelectedState(feeType: FeeType): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state

        val updatedFeeSelectorState = feeSelectorState.copy(selectedFee = feeType)
        val fee = updatedFeeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee, feeState.isSubtract)

        val updatedState = feeState.copy(
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            feeSelectorState = updatedFeeSelectorState,
            isPrimaryButtonEnabled = updatedFeeSelectorState.isPrimaryButtonEnabled(),
        )

        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(feeState = updatedState),
            ),
        )
    }

    fun onCustomFeeValueChange(index: Int, value: String): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val updatedFeeSelectorState = feeSelectorState.copy(
            customValues = feeSelectorState.customValues.toMutableList().apply {
                set(index, feeSelectorState.customValues[index].copy(value = value))
            }.toImmutableList(),
        )

        val fee = updatedFeeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee, feeState.isSubtract)

        val updatedState = feeState.copy(
            feeSelectorState = updatedFeeSelectorState,
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            isPrimaryButtonEnabled = updatedFeeSelectorState.isPrimaryButtonEnabled(),
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(feeState = updatedState),
            ),
        )
    }

    fun onSubtractSelect(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state

        val fee = feeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee, value)
        val updatedState = feeState.copy(
            isSubtract = value,
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = if (value) {
                getFormattedValue(receivedAmount)
            } else {
                feeState.receivedAmount
            },
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(feeState = updatedState),
            ),
        )
    }

    private fun FeeSelectorState.isPrimaryButtonEnabled(): Boolean {
        return when (this) {
            is FeeSelectorState.Loading -> false
            is FeeSelectorState.Content -> {
                val customValue = customValues.firstOrNull()?.value?.toBigDecimalOrNull()
                val balance = cryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
                val fee = getFee().amount.value ?: BigDecimal.ZERO

                val isNotEmptyCustom = !customValue.isNullOrZero() && selectedFee == FeeType.CUSTOM
                val isNotCustom = selectedFee != FeeType.CUSTOM
                fee < balance && (isNotEmptyCustom || isNotCustom)
            }
        }
    }

    private fun getFormattedValue(value: BigDecimal): String {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        return BigDecimalFormatter.formatCryptoAmount(
            cryptoAmount = value,
            cryptoCurrency = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
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