package com.tangem.data.pay.store

import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store of the last fetched [CustomerInfo] per [UserWalletId], alive for the process lifetime.
 *
 * Must be dropped together with the persisted TangemPay data when a wallet is removed, otherwise
 * re-adding the same wallet within one process keeps serving the previous account's info.
 */
@Singleton
internal class TangemPayCustomerInfoStore @Inject constructor() :
    RuntimeStateStore<Map<UserWalletId, CustomerInfo>> by RuntimeStateStore(defaultValue = emptyMap())