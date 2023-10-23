package com.tangem.domain.wallets.models

sealed class UnlockWalletsError {

    object UnableToUnlockWallets : UnlockWalletsError()

    object NoUserWalletSelected : UnlockWalletsError()

    object NotAllUserWalletsUnlocked : UnlockWalletsError()

    object NoUserWalletListManagerProvided : UnlockWalletsError()
}
