package com.tangem.tap.domain

import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.common.card.masks.Product
import java.util.*

object TapWorkarounds {

    var isStart2Coin: Boolean = false
        private set

    fun updateCard(card: Card) {
        isStart2Coin = card.cardData?.issuerName?.toLowerCase(Locale.US) == START_2_COIN_ISSUER
    }

    fun Card.isExcluded(): Boolean {
        val cardData = this.cardData ?: return false
        val productMask = cardData.productMask
        val excludedBatch = excludedBatches.contains(cardData.batchId)
        val excludedIssuerName = excludedIssuers.contains(cardData.issuerName?.toUpperCase(Locale.US))
        val excludedProductMask = (productMask != null && // product mask is on cards v2.30 and later
                !productMask.contains(Product.Note) && !productMask.contains(Product.TwinCard))
        return excludedBatch || excludedIssuerName || excludedProductMask

    }

    private const val START_2_COIN_ISSUER = "start2coin"

    private val excludedBatches = listOf(
                "0027",
                "0030",
                "0031",
        ) // Tangem tags

    private val excludedIssuers = listOf(
            "TTM BANK"
    )
}

val Card.isMultiwalletAllowed: Boolean
    get() {
        return cardData?.productMask?.contains(Product.TwinCard) != true
                && !TapWorkarounds.isStart2Coin
                && this.curve == EllipticCurve.Secp256k1
    }