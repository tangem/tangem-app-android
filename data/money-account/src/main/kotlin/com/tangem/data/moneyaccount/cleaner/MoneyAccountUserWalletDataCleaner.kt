package com.tangem.data.moneyaccount.cleaner

import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

internal class MoneyAccountUserWalletDataCleaner @Inject constructor(
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        moneyAccountStatusesStore.clear(userWalletIds)
    }
}