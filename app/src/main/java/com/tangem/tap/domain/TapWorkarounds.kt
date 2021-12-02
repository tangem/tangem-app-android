package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.twins.isTangemTwin
import java.util.*

object TapWorkarounds {

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.toLowerCase(Locale.US) == START_2_COIN_ISSUER
    }

    val Card.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val Card.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)

    fun Card.isExcluded(): Boolean {
        val excludedBatch = excludedBatches.contains(batchId)
        val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
        return excludedBatch || excludedIssuerName
    }

    fun Card.isNotSupportedInThatRelease():Boolean {
        return false
    }

    @Deprecated("Use ScanResponse.isTangemNote")
    fun isTangemNote(card: Card): Boolean = tangemNoteBatches.contains(card.batchId)

    @Deprecated("Use ScanResponse.isTangemWallet")
    fun isTangemWallet(card: Card): Boolean = tangemWalletBatches.contains(card.batchId)

    fun getTangemNoteBlockchain(card: Card): Blockchain? = tangemNoteBatches[card.batchId]

    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"

    private val excludedBatches = listOf(
            "0027",
            "0030",
            "0031",
            "0035"
    )

    private val excludedIssuers = listOf(
            "TTM BANK"
    )

    private val tangemWalletBatches = listOf("AC01")

    private val tangemNoteBatches = mapOf(
            "AB01" to Blockchain.Bitcoin,
            "AB02" to Blockchain.Ethereum,
            "AB03" to Blockchain.CardanoShelley,
            "AB04" to Blockchain.Dogecoin,
            "AB05" to Blockchain.BSC,
            "AB06" to Blockchain.XRP,
    )
}

val DELAY_SDK_DIALOG_CLOSE = 1400L

val Card.isMultiwalletAllowed: Boolean
    get() {
        return !isTangemTwin() && !isStart2Coin && !isTangemNote(this)
                && (firmwareVersion >= FirmwareVersion.MultiWalletAvailable ||
                getSingleWallet()?.curve == EllipticCurve.Secp256k1)
    }