package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class GetDynamicReceiveAddressUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, String> =
        Either.catch {
            dynamicAddressesRepository.getReceiveAddress(userWalletId, network)
        }
}