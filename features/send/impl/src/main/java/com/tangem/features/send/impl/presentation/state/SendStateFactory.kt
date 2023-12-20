package com.tangem.features.send.impl.presentation.state

import androidx.paging.PagingData
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.fee.calculateReceiveAmount
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientListConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.features.send.impl.presentation.viewmodel.isNotAddressInWallet
import com.tangem.features.send.impl.presentation.viewmodel.validateMemo
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val walletAddressesProvider: Provider<Set<Address>>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
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
    )

    fun getReadyState(): SendUiState = currentStateProvider().copy(
        amountState = amountStateConverter.convert(Unit),
        recipientState = recipientStateConverter.convert(Unit),
        feeState = feeStateConverter.convert(Unit),
        sendState = SendStates.SendState(),
    )
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
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateMemo(
            memo = recipientState.addressTextField.value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )
        val isAddressInWallet = isNotAddressInWallet(
            address = value,
            walletAddresses = walletAddressesProvider(),
        )
        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && isAddressInWallet,
                isValidating = false,
                addressTextField = recipientState.addressTextField.copy(
                    error = when {
                        !isValidAddress || !isAddressInWallet -> resourceReference(
                            R.string.send_recipient_address_error,
                        )
                        else -> null
                    },
                    isError = value.isNotEmpty() && !isValidAddress || !isAddressInWallet,
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
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateMemo(
            memo = value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )
        val isAddressInWallet = isNotAddressInWallet(
            walletAddresses = walletAddressesProvider(),
            address = recipientState.addressTextField.value,
        )
        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && isAddressInWallet,
                isValidating = false,
                memoTextField = recipientState.memoTextField?.copy(
                    isError = value.isNotEmpty() || isValidMemo && isValidAddress && isAddressInWallet,
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

    fun onFeeOnLoadedState(fees: TransactionFee, isMaxAmount: Boolean): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = FeeSelectorState.Content(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )
        val updatedState = feeState.copy(
            feeSelectorState = feeSelectorState,
            receivedAmount = feeSelectorState.updateReceiveAmount(),
            isSubtract = isMaxAmount,
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
        val updatedState = feeState.copy(
            receivedAmount = updatedFeeSelectorState.updateReceiveAmount(),
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
        val updatedState = feeState.copy(
            feeSelectorState = updatedFeeSelectorState,
            receivedAmount = updatedFeeSelectorState.updateReceiveAmount(),
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
        val updatedState = feeState.copy(
            isSubtract = value,
            receivedAmount = if (value) {
                feeSelectorState.updateReceiveAmount()
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
                val choosableFee = fees as? TransactionFee.Choosable
                val customValue = customValues.firstOrNull()?.value?.toBigDecimalOrNull()
                val balance = cryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
                val fee = when (selectedFee) {
                    FeeType.SLOW -> choosableFee?.minimum?.amount?.value
                    FeeType.MARKET -> fees.normal.amount.value
                    FeeType.FAST -> choosableFee?.priority?.amount?.value
                    FeeType.CUSTOM -> customValue
                } ?: BigDecimal.ZERO

                val isNotEmptyCustom = !customValue.isNullOrZero() && selectedFee == FeeType.CUSTOM
                val isNotCustom = selectedFee != FeeType.CUSTOM
                fee < balance && (isNotEmptyCustom || isNotCustom)
            }
        }
    }

    private fun FeeSelectorState.Content.updateReceiveAmount(): String {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        return BigDecimalFormatter.formatCryptoAmount(
            cryptoAmount = calculateReceiveAmount(currentStateProvider()),
            cryptoCurrency = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
        )
    }
    //endregion
}