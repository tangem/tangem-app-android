package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.CustomerOffersResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.visa.error.VisaApiError
import java.util.Currency
import javax.inject.Inject

internal class DefaultCustomerOffersRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOffersRepository {

    private val offersCache = RuntimeSharedStore<Map<String, List<Offer>>>()

    override suspend fun getOffers(userWalletId: UserWalletId): Either<VisaApiError, List<Offer>> {
        val key = userWalletId.stringValue
        offersCache.getSyncOrNull()?.get(key)?.let { return it.right() }

        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCustomerOffers(authHeader = authHeader)
        }.map { response ->
            response.result.map { it.toDomain() }
        }.onRight { offers ->
            offersCache.update(default = emptyMap()) { it + (key to offers) }
        }
    }

    private fun CustomerOffersResponse.Offer.toDomain(): Offer {
        return Offer(
            type = Offer.Type.fromString(type),
            fee = Offer.Fee(amount = fee.amount, currency = Currency.getInstance(fee.currency)),
            data = Offer.Data(
                specificationName = data.specificationName,
                orderType = OrderType.fromString(data.orderType),
            ),
        )
    }
}