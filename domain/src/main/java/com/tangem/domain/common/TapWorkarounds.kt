package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
object TapWorkarounds {
    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.lowercase(Locale.US) == START_2_COIN_ISSUER
    }

    val Card.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val Card.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)

    val Card.useOldStyleDerivation: Boolean
        get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"

    val Card.derivationStyle: DerivationStyle?
        get() = if (!settings.isHDWalletAllowed) {
            Int.MAX_VALUE
            null
        } else if (useOldStyleDerivation) {
            DerivationStyle.LEGACY
        } else {
            DerivationStyle.NEW
        }

    val Card.isNotSupportedInThatRelease: Boolean
        get() = false

    val Card.isTangemTwins: Boolean
        get() = TwinsHelper.getTwinCardNumber(cardId) != null
// [REDACTED_TODO_COMMENT]
    val Card.isTangemNote: Boolean
        get() = tangemNoteBatches.contains(batchId)

    private val tangemNoteBatches = mapOf(
        "AB01" to Blockchain.Bitcoin,
        "AB02" to Blockchain.Ethereum,
        "AB03" to Blockchain.CardanoShelley,
        "AB04" to Blockchain.Dogecoin,
        "AB05" to Blockchain.BSC,
        "AB06" to Blockchain.XRP,
        "AB07" to Blockchain.Bitcoin,
        "AB08" to Blockchain.Ethereum,
        "AB09" to Blockchain.Bitcoin,       // new batches for 3.34
        "AB10" to Blockchain.Ethereum,
        "AB11" to Blockchain.Bitcoin,
        "AB12" to Blockchain.Ethereum,
    )

    fun Card.getTangemNoteBlockchain(): Blockchain? = tangemNoteBatches[batchId] ?: null


    val Card.isSaltPay: Boolean
        get() = isSaltPayVisa || isSaltPayWallet

    val Card.isSaltPayVisa: Boolean
        get() = SaltPayWorkaround.isVisaBatchId(batchId)

    val Card.isSaltPayWallet: Boolean
        get() = SaltPayWorkaround.isWalletCardId(cardId)


    val Card.isExcluded: Boolean
        get() {
            val excludedBatch = excludedBatches.contains(batchId)
            val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
            return excludedBatch || excludedIssuerName
        }

    private val excludedBatches = listOf(
        "0027",
        "0030",
        "0031",
        "0035",
    )

    private val excludedIssuers = listOf(
        "TTM BANK",
    )
}
