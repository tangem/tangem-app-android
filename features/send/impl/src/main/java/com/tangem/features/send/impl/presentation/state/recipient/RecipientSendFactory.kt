package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toPersistentList

internal class RecipientSendFactory(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val isUtxoConsolidationAvailableProvider: Provider<Boolean>,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
) {
    private val recipientWalletListStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientWalletListConverter(
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            isUtxoConsolidationAvailableProvider = isUtxoConsolidationAvailableProvider,
        )
    }
    private val recipientHistoryListStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientHistoryListConverter(
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    fun onLoadedWalletsList(wallets: List<AvailableWallet?>): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            recipientState = state.recipientState?.copy(
                wallets = recipientWalletListStateConverter.convert(wallets),
            ),
        )
    }

    fun onLoadedHistoryList(txHistory: List<TxInfo>): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            recipientState = state.recipientState?.copy(
                recent = recipientHistoryListStateConverter.convert(txHistory),
            ),
        )
    }

    fun onRecipientAddressValueChange(value: String, isXAddress: Boolean = false, isValuePasted: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(
                addressTextField = recipientState.addressTextField.copy(value = value, isValuePasted = isValuePasted),
                memoTextField = recipientState.memoTextField?.copy(isEnabled = !isXAddress),
            ),
        )
    }

    fun getOnRecipientAddressValidState(value: String, maybeValidAddress: AddressValidationResult): SendUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state

        val isValidMemo = validateWalletMemoUseCase(
            memo = recipientState.memoTextField?.value.orEmpty(),
            network = cryptoCurrencyStatus.currency.network,
        ).isRight()

        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && maybeValidAddress.isRight(),
                isValidating = false,
                addressTextField = recipientState.addressTextField.copy(
                    error = maybeValidAddress.fold(
                        ifLeft = {
                            when (it) {
                                AddressValidation.Error.InvalidAddress -> resourceReference(
                                    R.string.send_recipient_address_error,
                                )
                                AddressValidation.Error.AddressInWallet -> resourceReference(
                                    R.string.send_error_address_same_as_wallet,
                                )
                                else -> null
                            }
                        },
                        ifRight = { null },
                    ),
                    isError = value.isNotEmpty() && maybeValidAddress.isLeft(),
                ),
            ),
        )
    }

    fun getOnRecipientAddressValidationStarted(): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(isValidating = true),
        )
    }

    fun getOnRecipientMemoValueChange(value: String, isValuePasted: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(
                memoTextField = recipientState.memoTextField?.copy(
                    value = value,
                    isValuePasted = isValuePasted,
                ),
            ),
        )
    }

    fun getOnRecipientMemoValidState(value: String, isValidAddress: Boolean): SendUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state

        val isValidMemo = validateWalletMemoUseCase(
            memo = value,
            network = cryptoCurrencyStatus.currency.network,
        ).isRight()

        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(
                isPrimaryButtonEnabled = isValidMemo && isValidAddress,
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
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            recipientState = recipientState.copy(
                memoTextField = recipientState.memoTextField?.copy(
                    value = "",
                    isEnabled = false,
                ),
            ),
        )
    }

    fun getHiddenRecentListState(isNotValid: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
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
}