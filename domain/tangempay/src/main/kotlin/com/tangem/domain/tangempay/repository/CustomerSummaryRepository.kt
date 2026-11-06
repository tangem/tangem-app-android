package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.error.TangemPayApiError

interface CustomerSummaryRepository {

    suspend fun fetch(userWalletId: UserWalletId): Either<TangemPayApiError, CustomerSummary>

    fun getFromCache(userWalletId: UserWalletId): CustomerSummary?
}