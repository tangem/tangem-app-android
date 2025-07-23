package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.domain.models.wallet.UserWalletId

class RunPolkadotAccountHealthCheckUseCase(
    private val polkadotAccountHealthCheckRepository: PolkadotAccountHealthCheckRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Unit> = Either.catch {
        polkadotAccountHealthCheckRepository.runCheck(userWalletId, network)
    }
}