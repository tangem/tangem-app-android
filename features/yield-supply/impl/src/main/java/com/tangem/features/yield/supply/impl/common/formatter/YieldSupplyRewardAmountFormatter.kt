package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Formats [amount] of this reward, which the backend reports in the token's base units.
 *
 * The reward surfaces each show a different amount of the same reward — pending, claimable, net of the service
 * fee — so the amount is passed in rather than picked here.
 */
internal fun YieldReward.formatAmount(amount: BigInteger): String {
    val fractionDigits = tokenDecimals.coerceAtMost(maximumValue = REWARD_MAX_DECIMALS)

    return amount.toBigDecimal().movePointLeft(tokenDecimals).format {
        crypto(symbol = symbol, decimals = tokenDecimals).anyDecimals(
            maxDecimals = fractionDigits,
            minDecimals = fractionDigits.coerceAtMost(maximumValue = REWARD_MIN_DECIMALS),
            roundingMode = RoundingMode.DOWN,
            canBeLower = true,
        )
    }
}

/** Past this many decimals a reward stops being readable; a token carrying more is cut here. */
private const val REWARD_MAX_DECIMALS = 8
private const val REWARD_MIN_DECIMALS = 2