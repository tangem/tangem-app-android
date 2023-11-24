package com.tangem.features.send.impl.presentation.state

import androidx.paging.PagingData
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.amount.SendAmountStateConverter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fee.SendFeeCustomFieldConverter
import com.tangem.features.send.impl.presentation.state.fee.SendFeeStateConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientListConverter
import com.tangem.features.send.impl.presentation.state.recipient.SendRecipientStateConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.features.send.impl.presentation.viewmodel.isNotAddressInWallet
import com.tangem.features.send.impl.presentation.viewmodel.validateMemo
import com.tangem.features.send.impl.presentation.viewmodel.verifyAddress
import com.tangem.utils.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val walletAddressesProvider: Provider<Set<Address>>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
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

    fun getOnRecipientAddressValueChangeState(value: String): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateMemo(
            memo = recipientState.addressTextField.value.value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )
        val isAddressInWallet = isNotAddressInWallet(
            address = value,
            walletAddresses = walletAddressesProvider(),
        )
        val isValidAddress = verifyAddress(
            address = value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )

        recipientState.addressTextField.update {
            it.copy(
                value = value,
                error = when {
                    !isValidAddress || !isAddressInWallet -> TextReference.Res(R.string.send_recipient_address_error)
                    else -> null
                },
                isError = !isValidAddress || !isAddressInWallet,
            )
        }
        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && isAddressInWallet,
            ),
        )
    }

    fun getOnRecipientMemoValueChangeState(value: String): SendUiState {
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        val isValidMemo = validateMemo(
            memo = value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )
        val isAddressInWallet = isNotAddressInWallet(
            walletAddresses = walletAddressesProvider(),
            address = recipientState.addressTextField.value.value,
        )
        val isValidAddress = verifyAddress(
            address = recipientState.addressTextField.value.value,
            cryptoCurrency = cryptoCurrencyStatusProvider().currency,
        )

        recipientState.memoTextField?.update {
            it.copy(
                value = value,
                isError = !isValidMemo,
            )
        }
        return state.copy(
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress && isAddressInWallet,
            ),
        )
    }

    fun onFeeOnLoadingState() {
        currentStateProvider().feeState?.feeSelectorState?.update {
            FeeSelectorState.Loading
        }
    }

    fun onFeeOnLoadedState(fees: TransactionFee) {
        currentStateProvider().feeState?.feeSelectorState?.update {
            FeeSelectorState.Content(
                fees = fees,
                customValues = customFeeFieldConverter.convert(fees.normal),
            )
        }
    }
    //endregion

    //region fee
    fun onFeeSelectedState(feeType: FeeType) {
        currentStateProvider().feeState?.feeSelectorState?.update {
            (it as? FeeSelectorState.Content)?.copy(selectedFee = feeType) ?: it
        }
    }
    //endregion
}