package com.tangem.domain.wallets.models

/**
[REDACTED_AUTHOR]
 */
sealed interface SaveWalletError {

    // TODO: Finalize in next PRs
    object CommonError : SaveWalletError
}