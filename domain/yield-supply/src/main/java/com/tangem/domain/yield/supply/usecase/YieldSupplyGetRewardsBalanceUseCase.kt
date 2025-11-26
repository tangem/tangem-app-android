package com.tangem.domain.yield.supply.usecase

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.ln

class YieldSupplyGetRewardsBalanceUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {

    operator fun invoke(status: CryptoCurrencyStatus, appCurrency: AppCurrency): Flow<String> = flow {
        val cryptoAmount = status.value.amount
        val fiatRate = status.value.fiatRate

        val amount = if (cryptoAmount != null && fiatRate != null) {
            cryptoAmount.multiply(fiatRate)
        } else {
            return@flow
        }

        val tokenAddress = (status.currency as? CryptoCurrency.Token)?.contractAddress ?: return@flow
        val apy = try {
            val markets = yieldSupplyRepository.getCachedMarkets() ?: yieldSupplyRepository.updateMarkets()
            markets.firstOrNull { it.tokenAddress.equals(tokenAddress, ignoreCase = true) }?.apy ?: BigDecimal.ZERO
        } catch (_: Throwable) {
            BigDecimal.ZERO
        }

        val apyFraction = apy.divide(HUNDRED_BD, SCALE, RoundingMode.HALF_UP)

        if (apyFraction.compareTo(BigDecimal.ZERO) == 0) {
            return@flow
        }

        val initialPerTickDelta = amount
            .multiply(apyFraction)
            .multiply(TICK_SECONDS_BD)
            .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)
            .abs()

        val minVisibleDecimals = calculateMinVisibleDecimals(initialPerTickDelta)

        var currentBalance: BigDecimal = amount
        while (true) {
            emit(
                currentBalance.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    ).anyDecimals(decimals = minVisibleDecimals)
                },
            )

            val perTickDelta = currentBalance
                .multiply(apyFraction)
                .multiply(TICK_SECONDS_BD)
                .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)

            currentBalance = currentBalance.add(perTickDelta)

            delay(TICK_MILLIS)
        }
    }.flowOn(dispatcherProvider.default)

    private fun calculateMinVisibleDecimals(perTickDeltaAbs: BigDecimal): Int {
        if (perTickDeltaAbs <= BigDecimal.ZERO) return MIN_DECIMALS

        val perTickAsDouble = perTickDeltaAbs.toDouble()
        if (perTickAsDouble.isNaN() || perTickAsDouble.isInfinite()) return MIN_DECIMALS

        val safe = if (perTickAsDouble <= 0.0) EPSILON else perTickAsDouble
        val raw = ceil(-ln(safe) / LN_10)
        return raw.toInt().coerceIn(MIN_DECIMALS, MAX_DECIMALS)
    }

    private companion object {
        const val TICK_MILLIS: Long = 300
        private val TICK_SECONDS_BD = BigDecimal("0.3")
        private val SECONDS_PER_YEAR_BD = BigDecimal("31536000") // 365 * 24 * 60 * 60
        private val HUNDRED_BD = BigDecimal("100")
        private const val SCALE = 18

        private const val MIN_DECIMALS = 3
        private const val MAX_DECIMALS = 8

        private val LN_10 = ln(10.0)
        private const val EPSILON = 1e-18
    }
}