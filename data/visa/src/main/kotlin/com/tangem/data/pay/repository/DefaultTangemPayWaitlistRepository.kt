package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.domain.models.pay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.repository.TangemPayWaitlistRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.WaitlistRequest
import javax.inject.Inject

internal class DefaultTangemPayWaitlistRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayWaitlistRepository {

    override suspend fun join(
        userWalletId: UserWalletId?,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<VisaApiError, Unit> {
        return requestHelper.performWithOptionalToken(userWalletId) { authHeader ->
            tangemPayApi.joinWaitlist(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = WaitlistRequest(
                    email = entry.email,
                    product = entry.product.id,
                    country = entry.countryCode,
                    region = entry.regionCode,
                    hasMarketingConsent = entry.isMarketingConsentGiven,
                ),
            )
        }.map {}
    }
}