package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnTokensCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class GetEarnTokensCatalogUseCase(
    private val repository: EarnRepository,
) {

    operator fun invoke(): Flow<EarnTokensCatalog?> {
        return repository.observeEarnTokensCatalog().distinctUntilChanged()
    }
}