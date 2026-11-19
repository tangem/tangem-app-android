package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.model.withVaultsIf
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnTokensCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetEarnTokensCatalogUseCase(
    private val repository: EarnRepository,
) {

    operator fun invoke(isVaultsIncluded: Boolean): Flow<EarnTokensCatalog?> {
        return repository.observeEarnTokensCatalog()
            .map { catalog -> catalog?.map { it.withVaultsIf(isVaultsIncluded) } }
            .distinctUntilChanged()
    }
}