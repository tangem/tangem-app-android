package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

interface CardTypesResolver {
    fun isTangemNote(): Boolean
    fun isTangemWallet(): Boolean
    fun isWallet2(): Boolean
    fun isTangemTwins(): Boolean
    fun isStart2Coin(): Boolean

    fun isMultiwalletAllowed(): Boolean

    fun getBlockchain(): Blockchain
    fun getPrimaryToken(): Token?
}