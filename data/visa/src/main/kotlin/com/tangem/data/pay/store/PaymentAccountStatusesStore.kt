package com.tangem.data.pay.store

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.PaymentAccountStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("UnusedParameter", "EmptyFunctionBlock", "FunctionOnlyReturningConstant")
@Singleton
internal class PaymentAccountStatusesStore @Inject constructor() {

    fun get(userWalletId: UserWalletId): Flow<PaymentAccountStatus> {
        return emptyFlow()
    }

    fun getSyncOrNull(userWalletId: UserWalletId): PaymentAccountStatus? {
        return null
    }

    fun store(userWalletId: UserWalletId, status: PaymentAccountStatus) {
    }
}