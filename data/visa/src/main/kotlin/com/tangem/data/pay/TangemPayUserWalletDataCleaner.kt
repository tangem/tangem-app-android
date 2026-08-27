package com.tangem.data.pay

import com.tangem.data.pay.repository.TangemPayRequestPerformer
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.data.pay.store.TangemPayCustomerInfoStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

internal class TangemPayUserWalletDataCleaner @Inject constructor(
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore,
    private val tangemPayStorage: TangemPayStorage,
    private val customerInfoStore: TangemPayCustomerInfoStore,
    private val requestPerformer: TangemPayRequestPerformer,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { clearStoredData(it) }

        paymentAccountStatusesStore.remove(userWalletIds)
        txHistoryItemsStore.remove(userWalletIds.map { it.stringValue })
        customerInfoStore.update { cache -> cache - userWalletIds }
        requestPerformer.removeCachedCustomerWalletAddresses(userWalletIds)
    }

    private suspend fun clearStoredData(userWalletId: UserWalletId) {
        // The auth tokens are keyed by the customer wallet address, so read it before clearAll wipes it.
        val customerWalletAddress = tangemPayStorage.getCustomerWalletAddress(userWalletId)

        tangemPayStorage.clearAll(userWalletId = userWalletId, customerWalletAddress = customerWalletAddress)
        tangemPayStorage.clearIsTangemPayDeactivated(userWalletId)
    }
}