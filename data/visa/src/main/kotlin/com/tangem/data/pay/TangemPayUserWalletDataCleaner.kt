package com.tangem.data.pay

import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.spend.datasource.pay.store.TangemPayTxHistoryItemsStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

internal class TangemPayUserWalletDataCleaner @Inject constructor(
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        paymentAccountStatusesStore.remove(userWalletIds)
        txHistoryItemsStore.remove(userWalletIds.map { it.stringValue })
    }
}