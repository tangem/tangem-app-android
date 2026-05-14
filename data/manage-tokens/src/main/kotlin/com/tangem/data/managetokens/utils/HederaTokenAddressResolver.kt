package com.tangem.data.managetokens.utils

import com.tangem.blockchain.blockchains.hedera.HederaContractIdResolver
import com.tangem.blockchain.blockchains.hedera.HederaTokenAddressConverter
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.providers.BlockchainProviderTypesStore
import java.util.concurrent.ConcurrentHashMap

internal class HederaTokenAddressResolver(
    private val blockchainProviderTypesStore: BlockchainProviderTypesStore,
) {
    private val tokenAddressConverter = HederaTokenAddressConverter()
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolveAddress(blockchain: Blockchain, contractAddress: String): String {
        cache[contractAddress.lowercase()]?.let { return it }

        val resolver = HederaContractIdResolver(baseUrl = getBaseUrl(blockchain))
        val resolved = tokenAddressConverter.resolveTokenId(contractAddress) { resolver.resolve(it) }

        require(!resolved.startsWith("0x", ignoreCase = true)) {
            "Failed to resolve Hedera contract ID for EVM address: $contractAddress"
        }

        cache[contractAddress.lowercase()] = resolved
        return resolved
    }

    private fun getBaseUrl(blockchain: Blockchain): String {
        val providerTypes = blockchainProviderTypesStore.get().value
        return requireNotNull(
            providerTypes[blockchain]
                ?.filterIsInstance<ProviderType.Public>()
                ?.firstOrNull()
                ?.url,
        ) {
            "Hedera provider URL not found for $blockchain"
        }
    }
}