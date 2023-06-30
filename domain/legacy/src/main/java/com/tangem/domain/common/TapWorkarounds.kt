package com.tangem.domain.common

import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.models.scan.CardDTO
import java.util.Locale

/**
[REDACTED_AUTHOR]
 */
@Deprecated(message = "Use CardTypeResolver. [REDACTED_JIRA]")
object TapWorkarounds {
    private const val START_2_COIN_ISSUER = "start2coin"
    private const val TEST_CARD_BATCH = "99FF"
    private const val TEST_CARD_ID_STARTS_WITH = "FF99"

    val CardDTO.isTangemTwins: Boolean
        get() = TwinsHelper.getTwinCardNumber(cardId) != null

    val CardDTO.isStart2Coin: Boolean
        get() = isStart2CoinIssuer(issuer.name)

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

    private val excludedBatches = listOf("0027", "0030", "0031", "0035")

    private val excludedIssuers = listOf("TTM BANK")

    fun isStart2CoinIssuer(cardIssuer: String?): Boolean {
        return cardIssuer?.lowercase(Locale.US) == START_2_COIN_ISSUER
    }
}