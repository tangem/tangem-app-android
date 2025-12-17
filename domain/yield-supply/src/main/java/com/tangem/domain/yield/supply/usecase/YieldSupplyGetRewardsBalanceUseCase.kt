package com.tangem.domain.yield.supply.usecase

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
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

    operator fun invoke(status: CryptoCurrencyStatus, appCurrency: AppCurrency): Flow<YieldSupplyRewardBalance> = flow {
        val cryptoAmount = status.value.amount
        val fiatRate = status.value.fiatRate

        if (cryptoAmount?.compareTo(BigDecimal.ZERO) == 0) {
            emit(
                YieldSupplyRewardBalance(fiatBalance = null, cryptoBalance = null),
            )
            return@flow
        }

        if (cryptoAmount == null) return@flow

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

        val initialPerTickDeltaCrypto = perTickDelta(cryptoAmount, apyFraction).abs()

        val minVisibleDecimalsCrypto = calculateMinVisibleDecimals(
            perTickDeltaAbs = initialPerTickDeltaCrypto,
            maxDecimals = status.currency.decimals,
        )

        val fiatAmountStart = fiatRate?.let { cryptoAmount.multiply(it) }
        val minVisibleDecimalsFiat = fiatAmountStart?.let { amount ->
            val initialPerTickDeltaFiat = perTickDelta(amount, apyFraction).abs()
            calculateMinVisibleDecimals(
                perTickDeltaAbs = initialPerTickDeltaFiat,
                maxDecimals = FIAT_MAX_DECIMALS,
            )
        }

        var currentCryptoBalance: BigDecimal = cryptoAmount
        var currentFiatBalance: BigDecimal? = fiatAmountStart
        while (true) {
            val fiatBalanceFormatted: String? = currentFiatBalance?.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ).anyDecimals(decimals = minVisibleDecimalsFiat ?: FIAT_MIN_DECIMALS)
            }

            val cryptoBalanceFormatted: String = currentCryptoBalance.format {
                crypto(status.currency).anyDecimals(
                    maxDecimals = minVisibleDecimalsCrypto,
                    minDecimals = minVisibleDecimalsCrypto,
                )
            }

            emit(
                YieldSupplyRewardBalance(fiatBalance = fiatBalanceFormatted, cryptoBalance = cryptoBalanceFormatted),
            )

            val perTickDeltaCrypto = perTickDelta(currentCryptoBalance, apyFraction)

            currentCryptoBalance = currentCryptoBalance.add(perTickDeltaCrypto)

            currentFiatBalance = currentFiatBalance?.let { current ->
                val perTickDeltaFiat = perTickDelta(current, apyFraction)
                current.add(perTickDeltaFiat)
            }

            delay(TICK_MILLIS)
        }
    }.flowOn(dispatcherProvider.default)

    private fun calculateMinVisibleDecimals(perTickDeltaAbs: BigDecimal, maxDecimals: Int): Int {
        if (perTickDeltaAbs <= BigDecimal.ZERO) return MIN_DECIMALS

        val perTickAsDouble = perTickDeltaAbs.toDouble()
        if (perTickAsDouble.isNaN() || perTickAsDouble.isInfinite()) return MIN_DECIMALS

        val safe = if (perTickAsDouble <= 0.0) EPSILON else perTickAsDouble
        val raw = ceil(-ln(safe) / LN_10)
        return raw.toInt().coerceIn(MIN_DECIMALS, maxDecimals)
    }

    private fun perTickDelta(amount: BigDecimal, apyFraction: BigDecimal): BigDecimal {
        return amount
            .multiply(apyFraction)
            .multiply(TICK_SECONDS_BD)
            .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)
    }

    companion object {
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
    }
}