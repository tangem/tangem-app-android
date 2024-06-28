package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

@Suppress("TooManyFunctions")
interface CardTypesResolver {

    fun isTangemNote(): Boolean

    fun isTangemWallet(): Boolean

    fun isWhiteWallet2(): Boolean

    fun isAvroraWallet(): Boolean

    fun isTraillantWallet(): Boolean

    fun isShibaWallet(): Boolean

    fun isTronWallet(): Boolean

    fun isKaspaWallet(): Boolean

    fun isKaspa2Wallet(): Boolean

    fun isKaspaResellerWallet(): Boolean

    fun isBadWallet(): Boolean

    fun isJrWallet(): Boolean

    fun isGrimWallet(): Boolean

    fun isSatoshiFriendsWallet(): Boolean

    fun isBitcoinPizzaDayWallet(): Boolean

    fun isVeChainWallet(): Boolean

    fun isNewWorldEliteWallet(): Boolean

    fun isRedPandaWallet(): Boolean

    fun isCryptoSethWallet(): Boolean

    fun isKishuInuWallet(): Boolean

    fun isBabyDogeWallet(): Boolean

    fun isCOQWallet(): Boolean

    fun isCoinMetricaWallet(): Boolean

    fun isVoltInuWallet(): Boolean

    fun isVividWallet(): Boolean

    fun isPastelWallet(): Boolean

    fun isWhiteWallet(): Boolean

    fun isWallet2(): Boolean

    fun isVisaWallet(): Boolean

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