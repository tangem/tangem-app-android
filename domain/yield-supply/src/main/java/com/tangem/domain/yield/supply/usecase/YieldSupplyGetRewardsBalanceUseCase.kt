package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.domain.yield.supply.usecase.RewardsBalanceTicker.Accrual
import com.tangem.domain.yield.supply.usecase.RewardsBalanceTicker.HUNDRED_BD
import com.tangem.domain.yield.supply.usecase.RewardsBalanceTicker.SCALE
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.math.BigDecimal
import java.math.RoundingMode

class YieldSupplyGetRewardsBalanceUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {

    operator fun invoke(status: CryptoCurrencyStatus, appCurrency: AppCurrency): Flow<YieldSupplyRewardBalance> = flow {
        val cryptoAmount = status.value.amount

        if (cryptoAmount?.compareTo(BigDecimal.ZERO) == 0) {
            emit(
                YieldSupplyRewardBalance(fiatBalance = null, cryptoBalance = null),
            )
            return@flow
        }

        if (cryptoAmount == null) return@flow

        val tokenAddress = (status.currency as? CryptoCurrency.Token)?.contractAddress ?: return@flow
        // The base rate, not the incentivised one: Merkl rewards accrue as separate reward tokens and never
        // compound into the supplied position this ticker extrapolates.
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

        emitAll(
            RewardsBalanceTicker.tick(
                fixedPart = BigDecimal.ZERO,
                accruing = listOf(Accrual(amount = cryptoAmount, rate = apyFraction)),
                currency = status.currency,
                fiatRate = status.value.fiatRate,
                appCurrency = appCurrency,
            ),
        )
    }.flowOn(dispatcherProvider.default)
}