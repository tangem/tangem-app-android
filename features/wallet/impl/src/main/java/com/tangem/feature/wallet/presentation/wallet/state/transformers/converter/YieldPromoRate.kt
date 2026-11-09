package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.feature.wallet.impl.R
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

private const val YIELD_PERCENT_POINT_SHIFT = 2

/** The backend sends a percent (5.5), the formatter takes the fraction it means (0.055). */
internal fun BigDecimal.toYieldPromoText(): TextReference {
    val rate = movePointLeft(YIELD_PERCENT_POINT_SHIFT).format { percent(canBeLower = true, withPercentSign = false) }
    return resourceReference(R.string.yield_module_main_screen_promo_banner_message, wrappedList(rate))
}

/**
 * The promo shows this rate whatever product wins the headline.
 * `null` for a coin and a token the map does not list.
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