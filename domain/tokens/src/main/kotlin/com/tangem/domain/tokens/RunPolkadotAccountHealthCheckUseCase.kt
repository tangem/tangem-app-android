package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class RunPolkadotAccountHealthCheckUseCase(
    private val polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Unit> =
        withContext(dispatchers.io) {
            Either.catch {
                polkadotAccountHealthCheckRepository.runCheck(userWalletId, network)
            }
        }
}