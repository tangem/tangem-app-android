package com.tangem.domain.walletconnect.usecase

import kotlinx.coroutines.flow.Flow

interface WcUseCasesFlowProvider {
    val useCases: Flow<WcUseCase>
}