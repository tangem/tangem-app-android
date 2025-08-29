package com.tangem.domain.core.wallets.error

sealed interface SetLockError {

    data object UserWalletNotFound : SetLockError

    data object UserWalletLocked : SetLockError

    data class UnableToSetLock(val cause: Throwable) : SetLockError
}