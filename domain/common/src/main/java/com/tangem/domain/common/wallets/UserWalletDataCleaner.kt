package com.tangem.domain.common.wallets

import com.tangem.domain.models.wallet.UserWalletId

/** Removes per-wallet data owned by a feature when its wallets are deleted. */
interface UserWalletDataCleaner {

    suspend fun clear(userWalletIds: List<UserWalletId>)
}