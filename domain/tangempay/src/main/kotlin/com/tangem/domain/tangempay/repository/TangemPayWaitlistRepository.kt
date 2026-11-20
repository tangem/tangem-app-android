package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayWaitlistEntry
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayWaitlistRepository {
    suspend fun join(
        userWalletId: UserWalletId?,
        product: TangemPayProduct,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Unit>
}