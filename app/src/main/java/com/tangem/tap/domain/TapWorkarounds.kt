package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.tap.domain.TapWorkarounds.isNote
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.twins.isTwinCard
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

    fun Card.isNote(): Boolean {
        return notesBatches.contains(batchId)
    }

    fun Card.isMultiCurrencyWallet(): Boolean {
        return multiCurrencyWalletsBatches.contains(batchId)
    }

    val Card.noteCurrency: Blockchain?
        get() {
            return when (batchId) {
                "AB01" -> Blockchain.Bitcoin
                "AB02" -> Blockchain.Ethereum
                "AB03" -> Blockchain.CardanoShelley
                "AB04" -> Blockchain.Dogecoin
                "AB05" -> Blockchain.Binance
                "AB06" -> Blockchain.XRP
                else -> null
            }
        }

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

    private val notesBatches = listOf(
        "AB01",
        "AB02",
        "AB03",
        "AB04",
        "AB05",
        "AB06",
    )

    private val multiCurrencyWalletsBatches = listOf("AC01")
}

val Card.isMultiwalletAllowed: Boolean
    get() {
        return !isTwinCard() && !isStart2Coin && !isNote()
                && (firmwareVersion >= FirmwareVersion.MultiWalletAvailable ||
                getSingleWallet()?.curve == EllipticCurve.Secp256k1)
    }