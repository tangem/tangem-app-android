package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError

/**
 * Loads the cashback summary from `GET /v1/customer/cashback/summary` and refreshes the cache read by
 * [ObserveCashbackSummaryUseCase].
 */
class GetCashbackSummaryUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary> {
        return cashbackRepository.getCashbackSummary(userWalletId)
    }
}