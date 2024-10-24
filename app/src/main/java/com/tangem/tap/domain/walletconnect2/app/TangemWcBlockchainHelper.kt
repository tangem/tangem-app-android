package com.tangem.tap.domain.walletconnect2.app

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.tap.domain.walletconnect2.domain.WcBlockchainHelper
import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.domain.walletconnect2.toggles.WalletConnectFeatureToggles

internal class TangemWcBlockchainHelper(
    featureToggles: WalletConnectFeatureToggles,
) : WcBlockchainHelper {

    private val supportedNonEvmBlockchains = if (featureToggles.isSolanaTxSignEnabled) {
        setOf(Blockchain.Solana, Blockchain.SolanaTestnet)
    } else {
        emptySet()
    }

    override fun chainIdToNetworkIdOrNull(chainId: String): String? {
        val parsedId = chainId.parseId() ?: return null
        val blockchain = parsedId.chainIdToBlockchain()

        return blockchain?.toNetworkId()
    }

    override fun chainIdToMissingNetworkNameOrNull(chainId: String): String? {
        val parsedId = chainId.parseId() ?: return null
        val blockchain = parsedId.chainIdToBlockchain()

        return blockchain?.fullName
            ?: if (parsedId.first == EVM_NAMESPACE) {
                chainId
            } else {
                parsedId.first.replaceFirstChar(Char::titlecase)
            }
    }

    override fun networkIdToChainIdOrNull(networkId: String): List<String> {
        val blockchain = Blockchain.fromNetworkId(networkId)
        val namespace = blockchain?.getCaip2Namespace() ?: return emptyList()
        return blockchain.getCaip2ChainIds().map {
            "$namespace$CHAIN_SEPARATOR$it"
        }
    }

    override fun getNamespaceFromFullChainIdOrNull(chainId: String): String? {
        val parsed = chainId.split(CHAIN_SEPARATOR)
        return parsed.firstOrNull()
    }

    override fun chainIdToFullNameOrNull(chainId: String): String? {
        val networkId = chainIdToNetworkIdOrNull(chainId) ?: return null
        return Blockchain.fromNetworkId(networkId)?.fullName
    }

    override fun chainIdsToAccounts(
        walletAddress: String,
        chainIds: List<String>,
        derivationPath: String?,
    ): List<Account> {
        return chainIds.map { chainId ->
            Account(chainId, walletAddress, derivationPath)
        }
    }

    private fun Blockchain.getCaip2ChainIds(): List<String> {
        if (this.isEvm()) return listOfNotNull(this.getChainId()?.toString())

        return when (this) {
            /*
             * The WC sample application and documentation use a commented out chain ID. However, in real dApps
             * uncommented is used.
             * Docs: https://docs.walletconnect.com/advanced/multichain/chain-list
             *
             * */
            Blockchain.Solana -> listOf("5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ")
            Blockchain.SolanaTestnet -> listOf("z4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z")
            Blockchain.Polkadot -> listOf("91b171bb158e2d3848fa23a9f1c25182")
            Blockchain.Tron -> listOf("0x2b6653dc")
            else -> emptyList()
        }
    }

    private fun Blockchain.getCaip2Namespace(): String? {
        return when {
            this.isEvm() -> EVM_NAMESPACE
            supportedNonEvmBlockchains.contains(this) -> this.toNetworkId().substringBefore(TESTNET_SEPARATOR)
            else -> null
        }
    }

    private fun String.parseId(): Pair<String, String>? {
        val parsed = this.split(CHAIN_SEPARATOR)
        if (parsed.size != 2) return null

        return parsed[0] to parsed[1]
    }

    private fun Pair<String, String>.chainIdToBlockchain(): Blockchain? {
        return when (first) {
            EVM_NAMESPACE -> {
                second.toIntOrNull()
                    ?.let(Blockchain::fromChainId)
            }
            else -> {
                Blockchain.fromNetworkId(networkId = first)
                    .takeIf(supportedNonEvmBlockchains::contains)
            }
        }
    }

    private companion object {
        const val EVM_NAMESPACE = "eip155"
        const val CHAIN_SEPARATOR = ":"
        const val TESTNET_SEPARATOR = "/"
    }
}
