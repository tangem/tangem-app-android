package com.tangem.data.tangempay.repository

import com.tangem.spend.datasource.tangempay.store.TangemPayIssueCardStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.TangemPayIssueCardRepository
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

internal class DefaultIssueCardRepository @Inject constructor(
    private val tangemPayIssueCardStore: TangemPayIssueCardStore,
) : TangemPayIssueCardRepository {

    override suspend fun storeIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        runSuspendCatching { tangemPayIssueCardStore.addIssueOrderId(userWalletId, orderId) }
    }

    override suspend fun getIssueOrderIds(userWalletId: UserWalletId): List<String> {
        return runSuspendCatching { tangemPayIssueCardStore.getIssueOrderIds(userWalletId) }
            .getOrDefault(emptyList())
    }

    override suspend fun removeIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        runSuspendCatching { tangemPayIssueCardStore.removeIssueOrderId(userWalletId, orderId) }
    }
}