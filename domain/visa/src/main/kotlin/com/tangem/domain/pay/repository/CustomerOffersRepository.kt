package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.visa.error.VisaApiError

/**
 * Repository for `GET /v1/customer/offers` and `GET /v1/product-instances/{id}/offers`.
 *
 * Used by the issue-additional-card flow to:
 * - check whether the additional-card offer is available;
 * - drive the popup amount via [Offer.fee].
 */
interface CustomerOffersRepository {

    suspend fun getOffers(userWalletId: UserWalletId): Either<VisaApiError, CustomerOffers>

    suspend fun getProductInstanceOffers(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, CustomerOffers>
}