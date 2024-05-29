package com.tangem.features.send.impl.presentation.state.recipient

import arrow.core.getOrElse
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

internal class RecipientSendFactory(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
) {
    private val recipientWalletListStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendRecipientWalletListConverter()
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

    fun onLoadedHistoryList(txHistory: List<TxHistoryItem>): SendUiState {
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

    fun getOnRecipientAddressValidState(value: String, isValidAddress: Boolean): SendUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state

        val isValidMemo = validateWalletMemoUseCase(
            memo = recipientState.memoTextField?.value.orEmpty(),
            network = cryptoCurrencyStatus.currency.network,
        ).getOrElse {
            Timber.e("Failed to validateWalletMemoUseCase: $it")
            false
        }
        val isAddressInWallet = cryptoCurrencyStatus.value.networkAddress?.availableAddresses
            ?.any { it.value == value } ?: true

        return state.copyWrapped(
            isEditState = isEditState,
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
        ).getOrElse {
            Timber.e("Failed to validateWalletMemoUseCase: $it")
            false
        }
        val isAddressInWallet = cryptoCurrencyStatus.value.networkAddress?.availableAddresses
            ?.any { it.value == value } ?: true

        return state.copyWrapped(
            isEditState = isEditState,
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

    fun getHiddenRecentListState(isAddressInWallet: Boolean, isValidAddress: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val recipientState = state.getRecipientState(isEditState) ?: return state
        val isNotValid = isAddressInWallet || !isValidAddress
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
