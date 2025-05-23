package com.tangem.data.walletconnect.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet

internal interface WcNamespaceConverter {

    val namespaceKey: NamespaceKey

    fun toBlockchain(chainId: CAIP2): Blockchain?
    fun toBlockchain(chainId: String): Blockchain? = toCAIP2(chainId)?.let { caip2 -> toBlockchain(caip2) }

    fun toCAIP2(network: Network): CAIP2?
    fun toCAIP2(chainId: String): CAIP2? = CAIP2.fromRaw(chainId)

    fun toNetwork(chainId: String, wallet: UserWallet): Network?
    fun toNetwork(chainId: String, wallet: UserWallet, excludedBlockchains: ExcludedBlockchains): Network? {
        val blockchain = toBlockchain(chainId) ?: return null
        return getNetwork(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = wallet.scanResponse,
            excludedBlockchains = excludedBlockchains,
        )
    }
}