package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.model.withVaultsIf
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnTopToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetTopEarnTokensUseCase(
    private val repository: EarnRepository,
) {

    operator fun invoke(isVaultsIncluded: Boolean): Flow<EarnTopToken?> {
        return repository.observeTopEarnTokens()
            .map { tokens -> tokens?.map { it.withVaultsIf(isVaultsIncluded) } }
            .distinctUntilChanged()
    }
}