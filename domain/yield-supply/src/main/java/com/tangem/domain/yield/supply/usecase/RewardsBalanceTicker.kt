package com.tangem.domain.yield.supply.usecase

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.ln

/**
 * A balance growing in real time: [fixedPart] stays, every [Accrual] compounds at its own yearly rate each tick. The
 * shown decimals are the first ones the growth of a tick reaches.
 */
internal object RewardsBalanceTicker {

    internal const val TICK_MILLIS: Long = 800
    internal val TICK_SECONDS_BD: BigDecimal = BigDecimal("0.8")
    internal val SECONDS_PER_YEAR_BD: BigDecimal = BigDecimal("31536000") // 365 * 24 * 60 * 60
    internal val HUNDRED_BD: BigDecimal = BigDecimal("100")
    internal const val SCALE: Int = 18

    internal const val MIN_DECIMALS: Int = 3
    internal const val FIAT_MIN_DECIMALS: Int = 2
    internal const val FIAT_MAX_DECIMALS: Int = 12

    internal val LN_10: Double = ln(10.0)
    internal const val EPSILON: Double = 1e-18

    /** @property rate yearly growth as a fraction */
    internal data class Accrual(val amount: BigDecimal, val rate: BigDecimal)

    fun fiatDecimals(accruing: List<Accrual>, fiatRate: BigDecimal): Int =
        calculateMinVisibleDecimals(tickGrowth(accruing).multiply(fiatRate), FIAT_MAX_DECIMALS)

    fun tick(
        fixedPart: BigDecimal,
        accruing: List<Accrual>,
        currency: CryptoCurrency,
        fiatRate: BigDecimal?,
        appCurrency: AppCurrency,
    ): Flow<YieldSupplyRewardBalance> = flow {
        val cryptoDecimals = calculateMinVisibleDecimals(tickGrowth(accruing), currency.decimals)
        val fiatDecimals = fiatRate?.let { fiatDecimals(accruing, it) }

        var parts = accruing
        while (true) {
            val cryptoTotal = fixedPart + parts.sumOf(Accrual::amount)
            emit(
                YieldSupplyRewardBalance(
                    fiatBalance = fiatRate?.let { rate ->
                        cryptoTotal.multiply(rate).format {
                            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                                .anyDecimals(decimals = fiatDecimals ?: FIAT_MIN_DECIMALS)
                        }
                    },
                    cryptoBalance = cryptoTotal.format {
                        crypto(currency).anyDecimals(maxDecimals = cryptoDecimals, minDecimals = cryptoDecimals)
                    },
                ),
            )
            parts = parts.map { it.copy(amount = it.amount + perTickDelta(it.amount, it.rate)) }
            delay(TICK_MILLIS)
        }
    }

    private fun tickGrowth(accruing: List<Accrual>): BigDecimal =
        accruing.sumOf { perTickDelta(it.amount, it.rate) }.abs()

    private fun calculateMinVisibleDecimals(perTickDeltaAbs: BigDecimal, maxDecimals: Int): Int {
        val effectiveMin = MIN_DECIMALS.coerceAtMost(maxDecimals)

        if (perTickDeltaAbs <= BigDecimal.ZERO) return effectiveMin

        val perTickAsDouble = perTickDeltaAbs.toDouble()
        if (perTickAsDouble.isNaN() || perTickAsDouble.isInfinite()) return effectiveMin

        val safe = if (perTickAsDouble <= 0.0) EPSILON else perTickAsDouble
        val raw = ceil(-ln(safe) / LN_10)
        return raw.toInt().coerceIn(effectiveMin, maxDecimals)
    }

    private fun perTickDelta(amount: BigDecimal, rate: BigDecimal): BigDecimal {
        return amount
            .multiply(rate)
            .multiply(TICK_SECONDS_BD)
            .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)
    }
}