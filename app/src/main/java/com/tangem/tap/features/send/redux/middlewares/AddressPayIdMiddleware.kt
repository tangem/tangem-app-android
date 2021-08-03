package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.common.services.Result
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification.SetAddressError
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification.SetWalletAddress
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.Error
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification.SetPayIdError
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification.SetPayIdWalletAddress
import com.tangem.tap.scope
import com.tangem.tap.store
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
        val potentialPayId = data.toLowerCase()
        if (isPayIdEnabled() && PayIdManager.isPayId(potentialPayId)) {
            dispatch(SetPayIdWalletAddress(potentialPayId, "", isUserInput))
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

        if (isPayIdEnabled() && PayIdManager.isPayId(addressPayId)) {
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
                        val addressDetails = result.data.getAddressDetails()
                        if (addressDetails == null) {
                            dispatch(SetPayIdError(Error.PAY_ID_NOT_REGISTERED))
                            return@withContext
                        }

                        val address = addressDetails.address
                        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(wallet, address)
                        if (failReason == null) {
                            dispatch(SetPayIdWalletAddress(payId, address, isUserInput))
                            dispatch(TransactionExtrasAction.Prepare(wallet.blockchain, address, addressDetails.tag))
                            dispatch(FeeAction.RequestFee)
                        } else {
                            dispatch(SetAddressError(failReason))
                            dispatch(TransactionExtrasAction.Release)
                        }
                    }
                    is Result.Failure -> {
                        dispatch(SetPayIdError(Error.PAY_ID_REQUEST_FAILED))
                        dispatch(TransactionExtrasAction.Release)
                    }
                }
            }
        }
    }

    private fun verifyAddress(address: String, wallet: Wallet, isUserInput: Boolean, dispatch: (Action) -> Unit) {
        val addressSchemeSplit = if (wallet.blockchain == Blockchain.BitcoinCash) {
            listOf(address)
        } else {
            address.split(":")
        }
        val noSchemeAddress = when (addressSchemeSplit.size) {
            1 -> address // no scheme
            2 -> { // scheme
                if (wallet.blockchain.validateShareScheme(addressSchemeSplit[0])) {
                    addressSchemeSplit[1]
                } else {
                    dispatch(SetAddressError(Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN))
                    return
                }
            }
            else -> { // invalid URI
                dispatch(SetAddressError(Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN))
                return
            }
        }

        val supposedAddress = noSchemeAddress.removeShareUriQuery() //TODO: parse query?

        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(wallet, supposedAddress)
        if (failReason == null) {
            noSchemeAddress.getQueryParameter("amount")?.toBigDecimalOrNull()?.let {
                dispatch(AmountAction.SetAmount(it, false))
                dispatch(AmountActionUi.CheckAmountToSend)
            }
            dispatch(SetWalletAddress(supposedAddress, isUserInput))
            dispatch(TransactionExtrasAction.Prepare(wallet.blockchain, address, null))
            dispatch(FeeAction.RequestFee)
        } else {
            dispatch(SetAddressError(failReason))
            dispatch(TransactionExtrasAction.Release)
        }
    }

    private fun isValidBlockchainAddressAndNotTheSameAsWallet(wallet: Wallet, address: String): Error? {
        return if (wallet.blockchain.validateAddress(address)) {
            if (wallet.addresses.all { it.value != address }) {
                null
            } else {
                Error.ADDRESS_SAME_AS_WALLET
            }
        } else {
            Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN
        }
    }

    private fun String.removeShareUriQuery(): String = this.substringBefore("?")
    private fun String.getQueryParameter(name: String): String? {
        return this.substringAfter("?").splitToMap("&", "=")[name]
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

        if (PayIdManager.isPayId(addressPayId) && isPayIdEnabled()) {
            verifyPayId(addressPayId, wallet, false, internalDispatcher)
        } else {
            verifyAddress(addressPayId, wallet, false, internalDispatcher)
        }
    }

    private fun isPayIdEnabled(): Boolean {
        return store.state.globalState.configManager?.config?.isSendingToPayIdEnabled ?: false
    }
}

fun String.splitToMap(firstDelimiter: String, secondDelimiter: String): Map<String, String> {
    return this.split(firstDelimiter)
            .map { it.split(secondDelimiter) }
            .map { it.first() to it.last().toString() }
            .toMap()
}