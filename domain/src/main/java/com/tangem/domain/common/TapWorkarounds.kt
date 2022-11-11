package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card
import java.util.*

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
object TapWorkarounds {
    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.lowercase(Locale.US) == START_2_COIN_ISSUER
    }

    val CardDTO.isTangemTwins: Boolean
        get() = TwinsHelper.getTwinCardNumber(cardId) != null

    //TODO: replace by reading files from a card
    val CardDTO.isTangemNote: Boolean
        get() = tangemNoteBatches.contains(batchId)

    val CardDTO.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val CardDTO.isSaltPay: Boolean
        get() = isSaltPayVisa || isSaltPayWallet

    val CardDTO.isSaltPayVisa: Boolean
        get() = SaltPayWorkaround.isVisaBatchId(batchId)

    val CardDTO.isSaltPayWallet: Boolean
        get() = SaltPayWorkaround.isWalletCardId(cardId)

    val CardDTO.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)

    val CardDTO.useOldStyleDerivation: Boolean
        get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"
    val CardDTO.derivationStyle: DerivationStyle?
        get() = if (!settings.isHDWalletAllowed) {
            null
        } else if (useOldStyleDerivation) {
            DerivationStyle.LEGACY
        } else {
            DerivationStyle.NEW
        }

    val CardDTO.isExcluded: Boolean
        get() {
            val excludedBatch = excludedBatches.contains(batchId)
            val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
            return excludedBatch || excludedIssuerName
        }

    val CardDTO.isNotSupportedInThatRelease: Boolean
        get() = false

    fun CardDTO.getTangemNoteBlockchain(): Blockchain? =
        tangemNoteBatches[batchId] ?: if (isSaltPay) Blockchain.Gnosis else null

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
