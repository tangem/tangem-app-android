package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import java.util.*

/**
[REDACTED_AUTHOR]
 */
object TapWorkarounds {

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

    private val tangemNoteBatches = mapOf(
        "AB01" to Blockchain.Bitcoin,
        "AB02" to Blockchain.Ethereum,
        "AB03" to Blockchain.CardanoShelley,
        "AB04" to Blockchain.Dogecoin,
        "AB05" to Blockchain.BSC,
        "AB06" to Blockchain.XRP,
        "AB07" to Blockchain.Bitcoin,
        "AB08" to Blockchain.Ethereum,
    )

    private val tangemWalletBatchesWithStandardDerivationType = listOf(
        "AC01", "AC02", "CB95"
    )

    fun Card.getTangemNoteBlockchain(): Blockchain? = tangemNoteBatches[batchId]

    val Card.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val Card.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)


    fun Card.isExcluded(): Boolean {
        val excludedBatch = excludedBatches.contains(batchId)
        val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
        return excludedBatch || excludedIssuerName
    }

    fun Card.isNotSupportedInThatRelease(): Boolean {
        return false
    }

    @Deprecated("Use ScanResponse.isTangemNote")
    fun isTangemNote(card: Card): Boolean = tangemNoteBatches.contains(card.batchId)


    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.toLowerCase(Locale.US) == START_2_COIN_ISSUER
    }
}