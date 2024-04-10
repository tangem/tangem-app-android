package com.tangem.tap.domain.walletconnect2.app

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.domain.walletconnect2.domain.WcBlockchainHelper
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

    override fun networkIdToChainIdOrNull(networkId: String): String? {
        val blockchain = Blockchain.fromNetworkId(networkId)
        val namespace = blockchain?.getCaip2Namespace() ?: return null
        val chainId = blockchain.getCaip2ChainId() ?: return null
        return "$namespace$CHAIN_SEPARATOR$chainId"
    }

    override fun getNamespaceFromFullChainIdOrNull(chainId: String): String? {
        val parsed = chainId.split(CHAIN_SEPARATOR)
        return parsed.firstOrNull()
    }

    override fun chainIdToFullNameOrNull(chainId: String): String? {
        val networkId = chainIdToNetworkIdOrNull(chainId) ?: return null
        return Blockchain.fromNetworkId(networkId)?.fullName
    }

    private fun Blockchain.getCaip2ChainId(): String? {
        if (this.isEvm()) return this.getChainId()?.toString()

        return when (this) {
            /*
             * The WC sample application and documentation use a commented out chain ID. However, in real dApps
             * uncommented is used.
             * Docs: https://docs.walletconnect.com/advanced/multichain/chain-list
             *
             * Blockchain.Solana -> "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
             * */
            Blockchain.Solana -> "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"
            Blockchain.SolanaTestnet -> "z4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z"
            Blockchain.Polkadot -> "91b171bb158e2d3848fa23a9f1c25182"
            Blockchain.Tron -> "0x2b6653dc"
            else -> null
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
