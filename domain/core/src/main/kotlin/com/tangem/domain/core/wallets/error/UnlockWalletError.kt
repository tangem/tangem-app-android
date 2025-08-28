package com.tangem.domain.core.wallets.error

sealed interface UnlockWalletError {

    data object AlreadyUnlocked : UnlockWalletError

    data object UserWalletNotFound : UnlockWalletError

    data object UnableToUnlock : UnlockWalletError

    data object UserCancelled : UnlockWalletError

    data object ScannedCardWalletNotMatched : UnlockWalletError
}