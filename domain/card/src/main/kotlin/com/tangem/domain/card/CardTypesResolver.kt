package com.tangem.domain.card

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

@Suppress("TooManyFunctions")
interface CardTypesResolver {

    fun isTangemNote(): Boolean

    fun isTangemWallet(): Boolean

    fun isShibaWallet(): Boolean

    fun isWhiteWallet(): Boolean

    fun isWallet2(): Boolean

    fun isVisaWallet(): Boolean

    fun isRing(): Boolean

    fun isTangemTwins(): Boolean

    fun isStart2Coin(): Boolean

    fun isDevKit(): Boolean

    fun isSingleWallet(): Boolean

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