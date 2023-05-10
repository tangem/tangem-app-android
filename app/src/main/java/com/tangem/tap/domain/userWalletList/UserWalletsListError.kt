package com.tangem.tap.domain.userWalletList

import com.tangem.common.core.TangemError
import com.tangem.wallet.R

sealed class UserWalletsListError(code: Int) : TangemError(code) {

    override val silent: Boolean
        get() = (cause as? TangemError)?.silent == true

    override val messageResId: Int? = null

    object WalletAlreadySaved : UserWalletsListError(code = 60001) {
        override var customMessage: String = "This wallet has already been saved, you can add another one"
        override val messageResId: Int = R.string.user_wallet_list_error_wallet_already_saved
    }

    object EncryptionKeyInvalidated : UserWalletsListError(code = 60002) {
        override var customMessage: String = "Encryption key invalidated"
    }

    object BiometricsAuthenticationDisabled : UserWalletsListError(code = 60005) {
        override var customMessage: String = "Biometrics authentication disabled"
    }

    data class BiometricsAuthenticationLockout(val isPermanent: Boolean) : UserWalletsListError(code = 60003) {
        override var customMessage: String = "Biometric authentication lockout, permanent: $isPermanent"
    }

    data class UnableToUnlockUserWallets(override val cause: Throwable? = null) : UserWalletsListError(code = 60004) {
        override var customMessage: String = "An error has occurred, please scan your card to log in"
        override val messageResId: Int = R.string.user_wallet_list_error_unable_to_unlock
    }
}
