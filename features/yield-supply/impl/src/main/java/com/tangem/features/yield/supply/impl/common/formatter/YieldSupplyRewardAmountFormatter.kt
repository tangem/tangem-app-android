package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.reward
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import java.math.BigInteger

/**
 * Formats [amount] of this reward, which the backend reports in the token's base units.
 *
 * The reward surfaces each show a different amount of the same reward — pending, claimable, net of the service
 * fee — so the amount is passed in rather than picked here.
 */
internal fun YieldReward.formatAmount(amount: BigInteger): String =
    amount.toBigDecimal().movePointLeft(tokenDecimals).format {
        crypto(symbol = symbol, decimals = tokenDecimals).reward()
    }