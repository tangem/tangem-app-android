package com.tangem.tap.domain.walletconnect2.app

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.domain.walletconnect2.domain.WcBlockchainHelper

class TangemWcBlockchainHelper : WcBlockchainHelper {
    override fun chainIdToNetworkIdOrNull(chainId: String): String? {
        val parsed = chainId.split(CHAIN_SEPARATOR)
        if (parsed.size != 2) return null

        return blockchainFromChainId(parsed[1])?.toNetworkId()
    }

    override fun networkIdToChainIdOrNull(networkId: String): String? {
        val blockchain = Blockchain.fromNetworkId(networkId)
        val namespace = blockchain?.getCaip2Namespace() ?: return null
        val chainId = blockchain.getCaip2ChainId() ?: return null
        return "$namespace$CHAIN_SEPARATOR$chainId"
    }

    override fun getNamespaceFromFullChainId(chainId: String): String? {
        val parsed = chainId.split(CHAIN_SEPARATOR)
        return parsed.firstOrNull()
    }

    override fun chainIdToFullNameOrNull(chainId: String): String? {
        val networkId = chainIdToNetworkIdOrNull(chainId) ?: return null
        return Blockchain.fromNetworkId(networkId)?.fullName
    }
}

private fun Blockchain.getCaip2ChainId(): String? {
    if (this.isEvm()) return this.getChainId()?.toString()

    return when (this) {
        Blockchain.Solana -> "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"
        Blockchain.SolanaTestnet -> "8E9rvCKLFQia2Y35HXjjpWzj8weVo44K"
        Blockchain.Polkadot -> "91b171bb158e2d3848fa23a9f1c25182"
        Blockchain.Tron -> "0x2b6653dc"
        else -> null
    }
}

private fun blockchainFromChainId(chainId: String): Blockchain? {
    val chainIdInt = chainId.toIntOrNull()
    if (chainIdInt != null) {
        return Blockchain.fromChainId(chainIdInt)
    }
    return when (chainId) {
        "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ" -> Blockchain.Solana
        "8E9rvCKLFQia2Y35HXjjpWzj8weVo44K" -> Blockchain.SolanaTestnet
        "91b171bb158e2d3848fa23a9f1c25182" -> Blockchain.Polkadot
        "0x2b6653dc" -> Blockchain.Tron
        else -> null
    }
}

private fun Blockchain.getCaip2Namespace(): String? {
    return when {
        this.isEvm() -> EVM_NAMESPACE
        supportedNonEvmBlockchains.contains(this) -> this.toNetworkId().removeSuffix("/test")
        else -> null
    }
}

private val supportedNonEvmBlockchains = setOf<Blockchain>(
    // Blockchain.Solana, Blockchain.SolanaTestnet, TODO: Enable when full support is established
    // Blockchain.Polkadot, Blockchain.Tron,
)
private const val EVM_NAMESPACE = "eip155"
private const val CHAIN_SEPARATOR = ":"