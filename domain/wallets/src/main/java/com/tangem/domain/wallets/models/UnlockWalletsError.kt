package com.tangem.domain.wallets.models

sealed class UnlockWalletsError {

    object UnableToUnlockWallets : UnlockWalletsError()

    object NoUserWalletSelected : UnlockWalletsError()

    object NotAllUserWalletsUnlocked : UnlockWalletsError()

    data class DataError(val cause: Throwable) : UnlockWalletsError()
}