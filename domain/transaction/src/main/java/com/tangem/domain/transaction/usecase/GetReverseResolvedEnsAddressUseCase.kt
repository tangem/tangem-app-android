package com.tangem.domain.transaction.usecase

import com.tangem.blockchain.common.ReverseResolveAddressResult
import com.tangem.domain.models.ens.EnsAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.WalletAddressServiceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class GetReverseResolvedEnsAddressUseCase(private val walletAddressServiceRepository: WalletAddressServiceRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        addresses: List<String>,
    ): List<EnsAddress> {
        return supervisorScope {
            val addressesDeferred = addresses.map { address ->
                async {
                    val result = walletAddressServiceRepository.reverseResolveAddress(
                        userWalletId = userWalletId,
                        network = network,
                        address = address,
                    )
                    mapReverseResolveResult(result)
                }
            }
            addressesDeferred.awaitAll()
        }
    }

    private fun mapReverseResolveResult(reverseResolveAddressResult: ReverseResolveAddressResult): EnsAddress {
        return when (reverseResolveAddressResult) {
            is ReverseResolveAddressResult.Error -> EnsAddress.Error(reverseResolveAddressResult.error)
            ReverseResolveAddressResult.NotSupported -> EnsAddress.NotSupported
            is ReverseResolveAddressResult.Resolved -> {
                if (reverseResolveAddressResult.name.isEmpty()) {
                    EnsAddress.NotSupported
                } else {
                    EnsAddress.Address(reverseResolveAddressResult.name)
                }
            }
        }
    }
}