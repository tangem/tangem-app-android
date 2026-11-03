package com.tangem.domain.common.wallets

import com.tangem.domain.models.wallet.UserWallet

const val MAX_HOT_WALLETS_COUNT = 5

fun List<UserWallet>.isHotWalletsLimitReached(): Boolean {
    return count { it is UserWallet.Hot } >= MAX_HOT_WALLETS_COUNT
}