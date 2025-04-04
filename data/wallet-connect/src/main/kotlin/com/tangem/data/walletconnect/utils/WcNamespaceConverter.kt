package com.tangem.data.walletconnect.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey

internal interface WcNamespaceConverter {

    val namespaceKey: NamespaceKey
    fun toBlockchain(chainId: CAIP2): Blockchain?
    fun toCAIP2(blockchain: Blockchain): CAIP2?
}