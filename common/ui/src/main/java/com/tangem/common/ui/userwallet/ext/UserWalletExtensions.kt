package com.tangem.common.ui.userwallet.ext

import com.tangem.common.ui.R
import com.tangem.domain.models.wallet.UserWallet

fun walletInterationIcon(userWallet: UserWallet): Int? {
    return when (userWallet) {
        is UserWallet.Cold -> R.drawable.ic_tangem_24
        is UserWallet.Hot -> null
    }
}