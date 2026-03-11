package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.repository.EarnRepository

class SetEarnFilterUseCase(private val repository: EarnRepository) {

    suspend operator fun invoke(filter: EarnFilter) {
        repository.setEarnFilter(filter)
    }
}