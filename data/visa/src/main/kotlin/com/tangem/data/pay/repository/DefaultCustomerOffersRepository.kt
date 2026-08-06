package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.OfferConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import javax.inject.Inject

internal class DefaultCustomerOffersRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOffersRepository {

    override suspend fun getOffers(userWalletId: UserWalletId): Either<VisaApiError, List<Offer>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCustomerOffers(authHeader = authHeader)
        }.map { response -> OfferConverter.convertList(response.result) }
    }
}