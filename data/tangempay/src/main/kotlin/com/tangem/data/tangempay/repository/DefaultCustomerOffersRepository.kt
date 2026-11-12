package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.data.tangempay.util.CustomerOffersConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerOffers
import com.tangem.domain.tangempay.repository.CustomerOffersRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import javax.inject.Inject

internal class DefaultCustomerOffersRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOffersRepository {

    override suspend fun getOffers(
        userWalletId: UserWalletId,
        deeplinkValue: String?,
    ): Either<TangemPayApiError, CustomerOffers> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCustomerOffers(authHeader = authHeader, deeplinkValue = deeplinkValue)
        }.map(CustomerOffersConverter::convert)
    }

    override suspend fun getProductInstanceOffers(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, CustomerOffers> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getProductInstanceOffers(authHeader = authHeader, productInstanceId = productInstanceId)
        }.map(CustomerOffersConverter::convert)
    }
}