package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import kotlin.text.ifEmpty

/**
 * Returns the dapp URL to show and verify based on the validation status and scam check.
 */
fun Wallet.Model.VerifyContext.getDappOriginUrl(): String {
    return if (this.validation == Wallet.Model.Validation.INVALID || this.isScam == true) {
        this.origin
    } else {
        this.origin.ifEmpty { this.verifyUrl }
    }
}