package com.tangem.feature.tester.presentation.backendauth

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Asks to link a wallet the way a feature behind mobile authentication does. */
internal class OpenWalletLinkSheetAction @Inject constructor(
    private val walletLinkRequester: WalletLinkRequester,
    private val appCoroutineScope: AppCoroutineScope,
) {

    /** The sheet is drawn by the main activity, so the request outlives the tester menu closed right after it. */
    operator fun invoke(userWallet: UserWallet) {
        appCoroutineScope.launch {
            val result = walletLinkRequester.requestLink(userWallet)
            TangemLogger.i("Tester wallet link request finished: $result")
        }
    }
}