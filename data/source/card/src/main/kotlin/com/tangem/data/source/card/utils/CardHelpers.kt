package com.tangem.data.source.card.utils

import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import java.util.*

private const val START_2_COIN_ISSUER = "start2coin"

private val excludedBatches = arrayOf("0027", "0030", "0031", "0035")
private val excludedIssuers = arrayOf("TTM BANK")

internal val Card.curvesForNonCreatedWallets: List<EllipticCurve> get() {
    val curvesPresent = wallets.map { it.curve }.toSet()
    val curvesForNonCreatedWallets = supportedCurves.subtract(
        curvesPresent + com.tangem.common.card.EllipticCurve.Secp256r1,
    )
    return curvesForNonCreatedWallets.toList()
}

internal val Card.useOldStyleDerivation: Boolean
    get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"

internal val Card.derivationStyle: DerivationStyle?
    get() = if (!settings.isHDWalletAllowed) {
        null
    } else if (useOldStyleDerivation) {
        DerivationStyle.LEGACY
    } else {
        DerivationStyle.NEW
    }

internal val Card.isExcluded: Boolean
    get() {
        val excludedBatch = excludedBatches.contains(batchId)
        val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
        return excludedBatch || excludedIssuerName
    }

internal val Card.isFirmwareMultiwalletAllowed: Boolean
    get() = firmwareVersion >= FirmwareVersion.MultiWalletAvailable && settings.maxWalletsCount > 1

@Suppress("UnusedReceiverParameter")
internal val Card.isNotSupportedInThatRelease: Boolean
    get() = false

internal val Card.isTangemTwins: Boolean
    get() = getTwinCardNumber(cardId) != null

internal val Card.isStart2Coin: Boolean
    get() = issuer.name.lowercase(Locale.US) == START_2_COIN_ISSUER

internal val Card.isSaltPay: Boolean
    get() = isSaltPayVisa || isSaltPayWallet

internal val Card.isSaltPayVisa: Boolean
    get() = isSaltPayVisaBatchId(batchId)

internal val Card.isSaltPayWallet: Boolean
    get() = isSaltPayWalletCardId(cardId)
