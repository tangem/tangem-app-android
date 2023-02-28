package com.tangem.tap.domain.userWalletList

import com.tangem.common.core.TangemError
import com.tangem.wallet.R

sealed class UserWalletListError(code: Int) : TangemError(code) {
    override val silent: Boolean
        get() = (cause as? TangemError)?.silent == true

    override val messageResId: Int? = null

    object WalletAlreadySaved : UserWalletListError(code = 60001) {
        override var customMessage: String = "This wallet has already been saved, you can add another one"
        override val messageResId: Int = R.string.user_wallet_list_error_wallet_already_saved
    }
}
