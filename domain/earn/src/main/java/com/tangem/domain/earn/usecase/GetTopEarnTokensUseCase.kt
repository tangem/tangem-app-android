package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnTopToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class GetTopEarnTokensUseCase(
    private val repository: EarnRepository,
) {

    operator fun invoke(): Flow<EarnTopToken> {
        return repository.observeTopEarnTokens().distinctUntilChanged()
    }
}