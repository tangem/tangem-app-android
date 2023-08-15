package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.models.scan.CardDTO
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
object TapWorkarounds {
    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"
    private val backupRequiredFirmwareVersion = FirmwareVersion(major = 6, minor = 21)

    val CardDTO.isTangemTwins: Boolean
        get() = TwinsHelper.getTwinCardNumber(cardId) != null

    val CardDTO.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val CardDTO.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)

    // for cards 6.21 and higher backup is not skippable
    val CardDTO.canSkipBackup: Boolean
        get() = this.firmwareVersion < backupRequiredFirmwareVersion

    val CardDTO.useOldStyleDerivation: Boolean
        get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"

    val CardDTO.isExcluded: Boolean
        get() {
            val excludedBatch = excludedBatches.contains(batchId)
            val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
            return excludedBatch || excludedIssuerName
        }

    val CardDTO.isNotSupportedInThatRelease: Boolean
        get() = false

    private val tangemNoteBatches = mapOf(
        "AB01" to Blockchain.Bitcoin,
        "AB02" to Blockchain.Ethereum,
        "AB03" to Blockchain.Cardano,
        "AB04" to Blockchain.Dogecoin,
        "AB05" to Blockchain.BSC,
        "AB06" to Blockchain.XRP,
        "AB07" to Blockchain.Bitcoin,
        "AB08" to Blockchain.Ethereum,
        "AB09" to Blockchain.Bitcoin, // new batches for 3.34
        "AB10" to Blockchain.Ethereum,
        "AB11" to Blockchain.Bitcoin,
        "AB12" to Blockchain.Ethereum,
    )

    private val excludedBatches = listOf("0027", "0030", "0031", "0035")

    private val excludedIssuers = listOf("TTM BANK")

    @Deprecated(
        "Now blockchain is read form files (CardTypesResolver.getBlockchain), " +
            "but for previously saved cards this method is still used",
    )
    fun CardDTO.getTangemNoteBlockchain(): Blockchain? = tangemNoteBatches[batchId]

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.lowercase(Locale.US) == START_2_COIN_ISSUER
    }

    fun Card.getTangemNoteBlockchain(): Blockchain? = tangemNoteBatches[batchId] ?: null
}
