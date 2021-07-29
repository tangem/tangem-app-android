package com.tangem.tap.domain

import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.common.card.masks.Product
import com.tangem.tap.domain.TapWorkarounds.isNote
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.extensions.getSingleWallet
import java.util.*

object TapWorkarounds {

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.toLowerCase(Locale.US) == START_2_COIN_ISSUER
    }

    val Card.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(cardData?.issuerName)

    fun Card.isExcluded(): Boolean {
        val cardData = this.cardData ?: return false
        val productMask = cardData.productMask
        val excludedBatch = excludedBatches.contains(cardData.batchId)
        val excludedIssuerName = excludedIssuers.contains(cardData.issuerName?.toUpperCase(Locale.US))
        val excludedProductMask = (productMask != null && // product mask is on cards v2.30 and later
                !productMask.contains(Product.Note) && !productMask.contains(Product.TwinCard))
        return excludedBatch || excludedIssuerName || excludedProductMask

    }

    fun Card.isNote(): Boolean {
        return notesBatches.contains(cardData?.batchId)
    }

    fun Card.isMultiCurrencyWallet(): Boolean {
        return multiCurrencyWalletsBatches.contains(cardData?.batchId)
    }

    private const val START_2_COIN_ISSUER = "start2coin"

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
        return cardData?.productMask?.contains(Product.TwinCard) != true
                && !isStart2Coin
                && !isNote()
                && (firmwareVersion.major >= 4 ||
                getSingleWallet()?.curve == EllipticCurve.Secp256k1)
    }