package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

@Suppress("TooManyFunctions")
interface CardTypesResolver {

    fun isTangemNote(): Boolean

    fun isTangemWallet(): Boolean

    fun isShibaWallet(): Boolean

    fun isTronWallet(): Boolean

    fun isKaspaWallet(): Boolean

    fun isBadWallet(): Boolean

    fun isWhiteWallet(): Boolean

    fun isWallet2(): Boolean

    fun isRing(): Boolean

    fun isTangemTwins(): Boolean

    fun isStart2Coin(): Boolean

    fun isDevKit(): Boolean

    fun isSingleWalletWithToken(): Boolean

    fun isMultiwalletAllowed(): Boolean

    fun getBlockchain(): Blockchain

    fun getPrimaryToken(): Token?

    fun isReleaseFirmwareType(): Boolean

    fun getRemainingSignatures(): Int?

    fun getCardId(): String

    fun isTestCard(): Boolean

    fun isAttestationFailed(): Boolean

    fun hasWalletSignedHashes(): Boolean
}
