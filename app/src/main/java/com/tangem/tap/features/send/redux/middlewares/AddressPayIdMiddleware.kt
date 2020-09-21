package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Wallet
import com.tangem.commands.common.network.Result
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification.SetAddressError
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification.SetWalletAddress
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.Error
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification.SetPayIdError
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification.SetPayIdWalletAddress
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.DispatchFunction

/**
[REDACTED_AUTHOR]
 */
internal class AddressPayIdMiddleware {

    fun handle(action: AddressPayIdActionUi, appState: AppState?, dispatch: (Action) -> Unit) {
        when (action) {
            is AddressPayIdActionUi.HandleUserInput -> handleUserInput(action.data, appState, dispatch)
            is AddressPayIdActionUi.PasteAddressPayId -> pasteAddressPayId(action.data, dispatch)
            is AddressPayIdActionUi.CheckClipboard -> verifyClipboard(action.data, appState, dispatch)
            is AddressPayIdActionUi.CheckAddressPayId -> verifyAddressPayId(appState, dispatch)
            else -> return
        }
    }

    private fun handleUserInput(input: String, appState: AppState?, dispatch: DispatchFunction) {
        val sendState = appState?.sendState ?: return
        if (input == sendState.addressPayIdState.viewFieldValue.value) return

        setAddressAndCheck(input, true, dispatch)
    }

    private fun pasteAddressPayId(data: String, dispatch: (Action) -> Unit) {
        setAddressAndCheck(data, false, dispatch)
    }

    private fun setAddressAndCheck(data: String, isUserInput: Boolean, dispatch: (Action) -> Unit) {
        if (PayIdManager.isPayId(data)) {
            dispatch(SetPayIdWalletAddress(data, "", isUserInput))
        } else {
            dispatch(SetWalletAddress(data, isUserInput))
        }
        dispatch(AddressPayIdActionUi.CheckAddressPayId)
    }

    private fun verifyAddressPayId(appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return
        val wallet = sendState.walletManager?.wallet ?: return
        val addressPayId = sendState.addressPayIdState.normalFieldValue ?: return
        val isUserInput = sendState.addressPayIdState.viewFieldValue.isFromUserInput

        if (PayIdManager.isPayId(addressPayId)) {
            verifyPayId(addressPayId, wallet, isUserInput, dispatch)
        } else {
            verifyAddress(addressPayId, wallet, isUserInput, dispatch)
        }
    }

    private fun verifyPayId(payId: String, wallet: Wallet, isUserInput: Boolean, dispatch: DispatchFunction) {
        val blockchain = wallet.blockchain
        if (!blockchain.isPayIdSupported()) {
            dispatch(SetPayIdError(Error.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN))
            return
        }

        scope.launch {
            val result = PayIdManager().verifyPayId(payId, blockchain)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        val address = result.data.getAddress()
                        if (address == null) {
                            dispatch(SetPayIdError(Error.PAY_ID_NOT_REGISTERED))
                            return@withContext
                        }

                        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(wallet, address)
                        if (failReason == null) {
                            dispatch(SetPayIdWalletAddress(payId, address, isUserInput))
                            dispatch(FeeAction.RequestFee)
                        } else {
                            dispatch(SetAddressError(failReason))
                        }
                    }
                    is Result.Failure -> {
                        dispatch(SetPayIdError(Error.PAY_ID_REQUEST_FAILED))
                    }
                }
            }
        }
    }

    private fun verifyAddress(address: String, wallet: Wallet, isUserInput: Boolean, dispatch: (Action) -> Unit) {
        val supposedAddress = extractAddressFromShareUri(address)

        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(wallet, supposedAddress)
        if (failReason == null) {
            dispatch(SetWalletAddress(supposedAddress, isUserInput))
        } else {
            dispatch(SetAddressError(failReason))
        }
    }

    private fun isValidBlockchainAddressAndNotTheSameAsWallet(wallet: Wallet, address: String): Error? {
        return if (wallet.blockchain.validateAddress(address)) {
            if (wallet.address != address) {
                null
            } else {
                Error.ADDRESS_SAME_AS_WALLET
            }
        } else {
            Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN
        }
    }

    //TODO: move to the blockchainSDK
    private fun extractAddressFromShareUri(shareUri: String): String {
        val sharePrefix = listOf("bitcoin:", "ethereum:", "ripple:")
        val prefixes = sharePrefix.filter { shareUri.contains(it) }
        return if (prefixes.isEmpty()) shareUri
        else shareUri.replace(prefixes[0], "")
    }

    private fun verifyClipboard(input: String?, appState: AppState?, dispatch: DispatchFunction) {
        val addressPayId = input ?: return
        val wallet = appState?.sendState?.walletManager?.wallet ?: return


        val internalDispatcher: (Action) -> Unit = {
            when (it) {
                is SetWalletAddress, is SetPayIdWalletAddress -> {
                    dispatch(AddressPayIdVerifyAction.ChangePasteBtnEnableState(true))
                }
                is SetAddressError, is SetPayIdError -> {
                    dispatch(AddressPayIdVerifyAction.ChangePasteBtnEnableState(false))
                }
            }
        }

        if (PayIdManager.isPayId(addressPayId)) {
            verifyPayId(addressPayId, wallet, false, internalDispatcher)
        } else {
            verifyAddress(addressPayId, wallet, false, internalDispatcher)
        }
    }
}