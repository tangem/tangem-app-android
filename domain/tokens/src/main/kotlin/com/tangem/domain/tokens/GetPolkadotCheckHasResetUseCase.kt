package com.tangem.domain.tokens

import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import kotlinx.coroutines.flow.Flow

class GetPolkadotCheckHasResetUseCase(
    private val polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
) {

    operator fun invoke(): Flow<Pair<String, Boolean>> =
        polkadotAccountHealthCheckRepository.subscribeToHasResetResults()
}