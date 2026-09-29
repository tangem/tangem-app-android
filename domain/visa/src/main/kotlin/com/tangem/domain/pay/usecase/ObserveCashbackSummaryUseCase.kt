package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import kotlinx.coroutines.flow.Flow

/** Cached cashback summary, updated by every [GetCashbackSummaryUseCase] call; `null` while nothing is cached. */
class ObserveCashbackSummaryUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<CashbackSummary?> {
        return cashbackRepository.observeCashbackSummary(userWalletId)
    }
}