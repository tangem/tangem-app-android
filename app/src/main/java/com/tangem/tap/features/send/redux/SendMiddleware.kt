package com.tangem.tap.features.send.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.features.send.redux.AddressPayIdActionUI.SetAddressOrPayId
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
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
    val sendAction = action as? SendScreenActionUI ?: return

    when (sendAction) {
        is AddressPayIdActionUI -> {
            when (sendAction) {
                is SetAddressOrPayId -> AddressPayIdHandler().handle(sendAction.data?.toString())
            }
        }
    }
}

internal class AddressPayIdHandler {
    fun handle(data: String?) {
        val walletManager = store.state.globalState.scanNoteResponse?.walletManager ?: return
        val clipboardData = data ?: return

        if (PayIdManager.isPayId(clipboardData)) {
            if (walletManager.wallet.blockchain.isPayIdSupported()) {
                store.dispatch(AddressPayIdAction.Verification.PayIdNotSupportedByBlockchain)
            } else {
                scope.launch {
                    val response = verifyPayID(walletManager, clipboardData)
                    if (response == null) {
                        store.dispatch(AddressPayIdAction.Verification.Failed)
                    } else {
                        val address = "some address D:" // extract from response
                        store.dispatch(AddressPayIdAction.Verification.Success(address))
                    }
                }
            }
        } else {

        }
    }

    private suspend fun verifyPayID(walletManager: WalletManager, payID: String?): String? {
        val cardId = walletManager.cardId
        val publicKey = store.state.globalState.scanNoteResponse?.card?.cardPublicKey

//        val result = PayIdManager().getPayId(cardId, publicKey.toHexString())
//        withContext(Dispatchers.Main) {
//            when (result) {
//                is Result.Success -> {
//                    val payId = result.data
//                    if (payId == null) {
//                        store.dispatch(WalletAction.LoadPayId.NotCreated)
//                    } else {
//                        store.dispatch(WalletAction.LoadPayId.Success(payId))
//                    }
//                }
//                is Result.Failure -> store.dispatch(WalletAction.LoadPayId.Failure)
//            }
//        }
        return null
    }

    private suspend fun verifyWalletAddress(payID: String?): Boolean {
        val isRealAddress = true
        return isRealAddress
    }
}
