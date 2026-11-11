package com.tangem.domain.tangempay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CashbackSummary
import com.tangem.domain.tangempay.repository.CashbackRepository
import kotlinx.coroutines.flow.Flow

/** Cached cashback summary, updated by every [GetCashbackSummaryUseCase] call; `null` while nothing is cached. */
class ObserveCashbackSummaryUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<CashbackSummary?> {
        return cashbackRepository.observeCashbackSummary(userWalletId)
    }
}