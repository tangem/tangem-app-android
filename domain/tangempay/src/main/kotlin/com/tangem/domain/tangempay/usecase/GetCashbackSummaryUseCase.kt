package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CashbackSummary
import com.tangem.domain.tangempay.repository.CashbackRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

/**
 * Loads the cashback summary from `GET /v1/customer/cashback/summary` and refreshes the cache read by
 * [ObserveCashbackSummaryUseCase].
 */
class GetCashbackSummaryUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, CashbackSummary> {
        return cashbackRepository.getCashbackSummary(userWalletId)
    }
}