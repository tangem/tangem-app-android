package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.visa.error.VisaApiError

/**
 * Repository for the cashback endpoints (`GET /v1/customer/cashback/...`).
 */
interface CashbackRepository {

    /** Loads the cashback summary for the customer of [userWalletId]. */
    suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary>
}