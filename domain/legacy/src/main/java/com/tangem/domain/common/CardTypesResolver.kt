package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

interface CardTypesResolver {

    fun isTangemNote(): Boolean

    fun isTangemWallet(): Boolean

    fun isShibaWallet(): Boolean

    fun isWhiteWallet(): Boolean

    fun isWallet2(): Boolean

    fun isTangemTwins(): Boolean

    fun isStart2Coin(): Boolean

    fun isDevKit(): Boolean

    fun isMultiwalletAllowed(): Boolean

    fun getBlockchain(): Blockchain

    fun getPrimaryToken(): Token?

    fun isReleaseFirmwareType(): Boolean

    fun getRemainingSignatures(): Int?

    fun getCardId(): String

    fun isTestCard(): Boolean

    fun isAttestationFailed(): Boolean

    fun hasWalletSignedHashes(): Boolean

    fun hasBackup(): Boolean

    fun isBackupForbidden(): Boolean
}
