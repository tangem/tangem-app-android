package com.tangem.domain.common.wallets.error

sealed interface UnlockWalletError {

    data object AlreadyUnlocked : UnlockWalletError

    data object UserWalletNotFound : UnlockWalletError

    sealed class UnableToUnlock : UnlockWalletError {

        data object Empty : UnableToUnlock()

        data class RawException(val throwable: Throwable) : UnableToUnlock()

        data class WithReason(val reason: Reason) : UnableToUnlock()

        sealed class Reason {
            data class BiometricsAuthenticationLockout(val isPermanent: Boolean) : Reason()
            data object AllKeysInvalidated : Reason()
            data object BiometricsAuthenticationDisabled : Reason()
        }
    }

    data object UserCancelled : UnlockWalletError

    data object ScannedCardWalletNotMatched : UnlockWalletError
}