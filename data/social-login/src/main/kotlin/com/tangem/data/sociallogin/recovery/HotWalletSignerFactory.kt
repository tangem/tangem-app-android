package com.tangem.data.sociallogin.recovery

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.lib.auth.session.WalletSigner

/** Signs with the wallet's own key through the Hot SDK, so the wallet does not have to be in the wallets list */
fun interface HotWalletSignerFactory {

    fun signerFor(userWallet: UserWallet.Hot): WalletSigner
}