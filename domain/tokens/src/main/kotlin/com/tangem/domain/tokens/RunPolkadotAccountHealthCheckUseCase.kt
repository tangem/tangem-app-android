package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class RunPolkadotAccountHealthCheckUseCase(
    private val polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network) = withContext(dispatchers.io) {
        polkadotAccountHealthCheckRepository.runCheck(userWalletId, network)
    }
}
