package com.tangem.data.jointaccount.cleaner

import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/** Removes the joint accounts of deleted wallets */
internal class JointAccountUserWalletDataCleaner @Inject constructor(
    private val accountsStore: JointAccountsStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        accountsStore.remove(userWalletIds)
    }
}