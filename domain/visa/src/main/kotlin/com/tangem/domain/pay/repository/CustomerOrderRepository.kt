package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.visa.error.VisaApiError

interface CustomerOrderRepository {

    suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderData>
}