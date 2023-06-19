package com.tangem.tap.domain.walletconnect2.domain

interface WcBlockchainHelper {
    fun chainIdToNetworkIdOrNull(chainId: String): String?

    fun networkIdToChainIdOrNull(networkId: String): String?

    fun getNamespaceFromFullChainIdOrNull(chainId: String): String?

    fun chainIdToFullNameOrNull(chainId: String): String?
}