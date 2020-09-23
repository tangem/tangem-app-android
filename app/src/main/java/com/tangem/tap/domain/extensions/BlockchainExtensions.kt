package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import org.stellar.sdk.requests.ErrorResponse


fun Blockchain.isNoAccountError(exception: Throwable): Boolean {
    return when (this) {
        Blockchain.Stellar -> {
            (exception is ErrorResponse) && (exception.code == 404)
        }
        Blockchain.XRP -> exception.message?.contains("Account not found") == true
        else -> false
    }
}

fun Blockchain.amountToCreateAccount(): Int? {
    return when (this) {
        Blockchain.Stellar -> 1
        Blockchain.XRP -> 20
        else -> null
    }
}
