package com.tangem.domain.card

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

interface CardTypeResolver {

    fun isTangemNote(): Boolean

    fun isTangemWallet(): Boolean

    fun isWallet2(): Boolean

    fun isTangemTwins(): Boolean

    fun isStart2Coin(): Boolean

    fun isMultiwalletAllowed(): Boolean

    fun getBlockchain(): Blockchain

    fun getPrimaryToken(): Token?
}