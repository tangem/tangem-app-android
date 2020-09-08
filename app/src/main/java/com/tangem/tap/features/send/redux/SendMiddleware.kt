package com.tangem.tap.features.send.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.common.network.Result
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.PayIdManager.Companion.isPayId
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.ChangeAddressOrPayId
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.*
import com.tangem.tap.features.send.redux.AmountActionUi.SetMainCurrency
import com.tangem.tap.features.send.redux.AmountActionUi.ToggleMainCurrency
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware

/**
* [REDACTED_AUTHOR]
 */
val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            handleSendAction(action)
            nextDispatch(action)
        }
    }
}

private fun handleSendAction(action: Action) {
    val sendAction = action as? SendScreenActionUi ?: return

    when (sendAction) {
        is AddressPayIdActionUi -> {
            when (sendAction) {
                is ChangeAddressOrPayId -> AddressPayIdHandler().handle(sendAction.data)
            }
        }
        is AmountActionUi -> {
            when (sendAction) {
                is ToggleMainCurrency -> {
                    if (store.state.sendState.amountState.mainCurrency.value == MainCurrencyType.FIAT) {
                        store.dispatch(SetMainCurrency(MainCurrencyType.CRYPTO))
                    } else {
                        store.dispatch(SetMainCurrency(MainCurrencyType.FIAT))
                    }
                }
            }
        }
    }
}

internal class AddressPayIdHandler {
    fun handle(data: String) {
        val walletManager = store.state.globalState.scanNoteResponse?.walletManager ?: return
        if (data == store.state.sendState.addressPayIdState.etFieldValue) return

        if (isPayId(data)) {
            verifyPayId(data, walletManager)
        } else {
            val supposedAddress = extractAddressFromShareUri(data)
            store.dispatch(PayIdVerification.SetError(supposedAddress, FailReason.IS_NOT_PAY_ID))
            verifyAddress(walletManager, supposedAddress)
        }
    }

    private fun verifyPayId(payId: String, walletManager: WalletManager) {
        val blockchain = walletManager.wallet.blockchain
        if (!blockchain.isPayIdSupported()) {
            store.dispatch(PayIdVerification.SetError(payId, FailReason.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN))
            return
        }

        scope.launch {
            val result = PayIdManager().verifyPayId(payId, blockchain)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        val address = result.data.getAddress()
                        if (address == null) {
                            store.dispatch(PayIdVerification.SetError(payId, FailReason.PAY_ID_NOT_REGISTERED))
                            return@withContext
                        }
                        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(walletManager.wallet, address)

                        val actionToSend = if (failReason == FailReason.NONE) PayIdVerification.SetPayIdWalletAddress(payId, address)
                        else AddressVerification.SetError(payId, failReason)
                        store.dispatch(actionToSend)
                    }
                    is Result.Failure -> {
                        store.dispatch(PayIdVerification.SetError(payId, FailReason.PAY_ID_REQUEST_FAILED))
                    }
                }

            }
        }
    }

    private fun verifyAddress(walletManager: WalletManager, supposedAddress: String) {
        val failReason = isValidBlockchainAddressAndNotTheSameAsWallet(walletManager.wallet, supposedAddress)
        val actionToSend = if (failReason == FailReason.NONE) AddressVerification.SetWalletAddress(supposedAddress)
        else AddressVerification.SetError(supposedAddress, failReason)

        store.dispatch(actionToSend)
    }

    private fun isValidBlockchainAddressAndNotTheSameAsWallet(wallet: Wallet, address: String): FailReason {
        return if (wallet.blockchain.validateAddress(address)) {
            if (wallet.address != address) {
                FailReason.NONE
            } else {
                FailReason.ADDRESS_SAME_AS_WALLET
            }
        } else {
            FailReason.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN
        }
    }
// [REDACTED_TODO_COMMENT]
    private fun extractAddressFromShareUri(shareUri: String): String {
        val sharePrefix = listOf("bitcoin:", "ethereum:", "ripple:")
        val prefixes = sharePrefix.filter { shareUri.contains(it) }
        return if (prefixes.isEmpty()) shareUri
        else shareUri.replace(prefixes[0], "")
    }
}
