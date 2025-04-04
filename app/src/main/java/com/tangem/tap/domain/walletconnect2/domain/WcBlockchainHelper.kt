package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.walletconnect.model.legacy.Account

interface WcBlockchainHelper {
    fun chainIdToNetworkIdOrNull(chainId: String): String?

    fun chainIdToMissingNetworkNameOrNull(chainId: String): String?

    fun networkIdToChainIdOrNull(networkId: String): List<String>

    fun getNamespaceFromFullChainIdOrNull(chainId: String): String?

    fun chainIdToFullNameOrNull(chainId: String): String?

    fun chainIdsToAccounts(walletAddress: String, chainIds: List<String>, derivationPath: String?): List<Account>

    fun chainIdsToBlockchains(chainIds: List<String>): List<Blockchain>
}