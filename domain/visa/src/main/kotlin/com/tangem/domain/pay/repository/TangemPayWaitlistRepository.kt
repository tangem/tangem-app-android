package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayWaitlistRepository {
    suspend fun join(
        userWalletId: UserWalletId?,
        product: TangemPayProduct,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<VisaApiError, Unit>
}