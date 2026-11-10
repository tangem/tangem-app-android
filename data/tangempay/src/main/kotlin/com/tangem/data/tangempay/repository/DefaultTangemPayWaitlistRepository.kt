package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayWaitlistEntry
import com.tangem.domain.tangempay.repository.TangemPayWaitlistRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.models.request.WaitlistRequest
import javax.inject.Inject

internal class DefaultTangemPayWaitlistRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayWaitlistRepository {

    override suspend fun join(
        userWalletId: UserWalletId?,
        product: TangemPayProduct,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Unit> {
        return requestHelper.performWithOptionalToken(userWalletId) { authHeader ->
            tangemPayApi.joinWaitlist(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = WaitlistRequest(
                    email = entry.email,
                    product = product.id,
                    country = entry.countryCode,
                    region = entry.regionCode,
                    hasMarketingConsent = entry.isMarketingConsentGiven,
                ),
            )
        }.map {}
    }
}