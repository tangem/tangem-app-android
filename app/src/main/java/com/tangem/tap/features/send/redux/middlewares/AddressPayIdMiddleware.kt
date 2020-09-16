package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.common.network.Result
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.*
import com.tangem.tap.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.DispatchFunction

/**
[REDACTED_AUTHOR]
 */
internal class AddressPayIdMiddleware {
    fun handle(data: String?, appState: AppState?, dispatch: DispatchFunction) {
        if (data == null) {
            dispatch(AddressVerification.SetError("", Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN))
            return
        }

        val sendState = appState?.sendState ?: return
        val walletManager = sendState.walletManager ?: return
        if (data == sendState.addressPayIdState.etFieldValue) return

        if (PayIdManager.isPayId(data)) {
            verifyPayId(data, walletManager, dispatch)
        } else {
            val supposedAddress = extractAddressFromShareUri(data)
            dispatch(PayIdVerification.SetError(supposedAddress, Error.IS_NOT_PAY_ID))

            val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(walletManager.wallet, supposedAddress)
            if (failReason == null) {
                dispatch(AddressVerification.SetWalletAddress(supposedAddress))
            } else {
                dispatch(AddressVerification.SetError(supposedAddress, failReason))
            }
        }
    }

    private fun verifyPayId(payId: String, walletManager: WalletManager, dispatch: DispatchFunction) {
        val blockchain = walletManager.wallet.blockchain
        if (!blockchain.isPayIdSupported()) {
            dispatch(PayIdVerification.SetError(payId, Error.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN))
            return
        }

        scope.launch {
            val result = PayIdManager().verifyPayId(payId, blockchain)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        val address = result.data.getAddress()
                        if (address == null) {
                            dispatch(PayIdVerification.SetError(payId, Error.PAY_ID_NOT_REGISTERED))
                            return@withContext
                        }
                        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(walletManager.wallet, address)

                        if (failReason == null) {
                            dispatch(PayIdVerification.SetPayIdWalletAddress(payId, address))
                        } else {
                            dispatch(AddressVerification.SetError(payId, failReason))
                        }

                    }
                    is Result.Failure -> {
                        dispatch(PayIdVerification.SetError(payId, Error.PAY_ID_REQUEST_FAILED))
                    }
                }
            }
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
}