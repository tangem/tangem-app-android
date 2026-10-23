package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.visa.error.VisaApiError

interface CustomerSummaryRepository {

    suspend fun fetch(userWalletId: UserWalletId): Either<VisaApiError, CustomerSummary>

    fun getFromCache(userWalletId: UserWalletId): CustomerSummary?
}