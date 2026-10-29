package com.tangem.data.pay.store

import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayCustomerSummaryStore @Inject constructor() :
    RuntimeStateStore<Map<UserWalletId, TangemPayCustomerSummaryStore.Entry>> by RuntimeStateStore(
        defaultValue = emptyMap(),
    ) {

    data class Entry(val summary: CustomerSummary, val fetchedAt: Long)
}