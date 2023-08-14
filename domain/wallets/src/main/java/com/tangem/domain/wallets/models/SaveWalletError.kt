package com.tangem.domain.wallets.models

/**
[REDACTED_AUTHOR]
 */
sealed interface SaveWalletError {

    object CommonError : SaveWalletError
}