package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.repository.EarnRepository
import kotlinx.coroutines.flow.Flow

class GetEarnFilterUseCase(private val repository: EarnRepository) {

    operator fun invoke(): Flow<EarnFilter> {
        return repository.observeEarnFilter()
    }
}