package com.tangem.tap.domain.walletconnect2.domain

interface WcBlockchainHelper {
    fun chainIdToNetworkIdOrNull(chainId: String): String?

    fun networkIdToChainIdOrNull(networkId: String): String?

    fun getNamespaceFromFullChainId(chainId: String): String?

    fun chainIdToFullNameOrNull(chainId: String): String?
}