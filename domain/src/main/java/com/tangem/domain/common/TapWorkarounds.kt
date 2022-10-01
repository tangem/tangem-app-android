package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.common.card.Card
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
object TapWorkarounds {
    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.lowercase(Locale.US) == START_2_COIN_ISSUER
    }

    val Card.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

    val Card.isSaltPay: Boolean
        get() = isSaltPayVisa || isSaltPayTangem

    val Card.isSaltPayVisa: Boolean
        get() = saltPayVisaBatches.contains(batchId)

    val Card.isSaltPayTangem: Boolean
        get() = saltPayTangemCardIds.contains(cardId)

    val Card.isTestCard: Boolean
        get() = batchId == TEST_CARD_BATCH && cardId.startsWith(TEST_CARD_ID_STARTS_WITH)
    val Card.useOldStyleDerivation: Boolean
        get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"
    val Card.derivationStyle: DerivationStyle?
        get() = if (!settings.isHDWalletAllowed) {
            null
        } else if (useOldStyleDerivation) {
            DerivationStyle.LEGACY
        } else {
            DerivationStyle.NEW
        }

    fun Card.isExcluded(): Boolean {
        val excludedBatch = excludedBatches.contains(batchId)
        val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
        return excludedBatch || excludedIssuerName
    }

    fun Card.isNotSupportedInThatRelease(): Boolean {
        return false
    }

    fun Card.isTangemNote(): Boolean = tangemNoteBatches.contains(batchId)
    fun isTangemWalletBatch(card: Card): Boolean = tangemWalletBatches.contains(card.batchId)
    fun Card.getTangemNoteBlockchain(): Blockchain? =
        tangemNoteBatches[batchId] ?: null

    fun Card.getSaltPayBlockchain(): Blockchain = Blockchain.SaltPay

    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"
    private val excludedBatches = listOf(
        "0027",
        "0030",
        "0031",
        "0035",
    )

    private val excludedIssuers = listOf(
        "TTM BANK",
    )

    private val tangemWalletBatches = listOf("AC01")

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

    private val tangemWalletBatchesWithStandardDerivationType = listOf(
        "AC01", "AC02", "CB95",
    )

    val saltPayTangemCardIds = listOf(
        "AC01000000000015", // TODO: remove testing CIDs
        "AC03000000000088", // TODO: remove testing CIDs
        "AC03000000076070",
        "AC03000000076088",
        "AC03000000076096",
        "AC03000000076104",
        "AC03000000076112",
        "AC03000000076120",
        "AC03000000076138",
        "AC03000000076146",
        "AC03000000076153",
        "AC03000000076161",
        "AC03000000076179",
        "AC03000000076187",
        "AC03000000076195",
        "AC03000000076203",
        "AC03000000076211",
        "AC03000000076229",
        // added 01.10.2022
        "AC01000000033503",
        "AC01000000033594",
        "AC01000000033586",
        "AC01000000034477",
        "AC01000000032760",
        "AC01000000033867",
        "AC01000000032653",
        "AC01000000032752",
        "AC01000000034485",
        "AC01000000033644",
        "AC01000000037454",
        "AC01000000037462",
    )

    private val saltPayVisaBatches = listOf(
        "AE02",
        "AE03",
    )

    val saltPayToken: Token
        get() = Token(
            name = "Wrapped xDAI",
            symbol = "WxDAI",
            contractAddress = "0x4346186e7461cB4DF06bCFCB4cD591423022e417",
            decimals = 18,
            id = "xdai",
        )
}
