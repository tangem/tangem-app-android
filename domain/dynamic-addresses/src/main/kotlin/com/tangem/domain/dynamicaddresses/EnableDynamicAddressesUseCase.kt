package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class EnableDynamicAddressesUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, xpub: String): Either<Throwable, Unit> =
        Either.catch {
            dynamicAddressesRepository.enable(userWalletId, network, xpub)
        }
}