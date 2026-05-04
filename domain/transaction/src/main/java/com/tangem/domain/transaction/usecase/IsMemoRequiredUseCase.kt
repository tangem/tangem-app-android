package com.tangem.domain.transaction.usecase

import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.WalletAddressServiceRepository

class IsMemoRequiredUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
) {

    suspend operator fun invoke(network: Network, destinationAddress: String): Boolean {
        return try {
            walletAddressServiceRepository.isMemoRequired(
                network = network,
                destinationAddress = destinationAddress,
            )
        } catch (_: Throwable) {
            false
        }
    }
}