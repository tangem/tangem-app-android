package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

/** Decimal places between a backend percent (5.5) and the fraction it means (0.055). */
internal const val YIELD_PERCENT_POINT_SHIFT = 2

internal fun BigDecimal.formatEarnRate(): String = format { percent(canBeLower = true, withPercentSign = false) }

/**
 * The Yield Mode rate of a token in percent, as the backend reports it, or `null` for a coin and a token the map does
 * not list. The promo bubble shows this rate whatever product wins the headline.
 */
internal fun yieldApyPercentOf(currency: CryptoCurrency, yieldModuleApyMap: Map<String, BigDecimal>): BigDecimal? {
    val token = currency as? CryptoCurrency.Token ?: return null
    return yieldModuleApyMap.entries
        .firstOrNull { (key, _) ->
            key.equals(
                other = token.yieldSupplyKey(),
                ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId),
            )
        }
        ?.value
}