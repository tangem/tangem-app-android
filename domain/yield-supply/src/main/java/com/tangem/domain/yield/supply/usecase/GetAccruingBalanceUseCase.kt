package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.accruingContributions
import com.tangem.domain.models.currency.balance.totalContributionsDeltaOrNull
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.domain.yield.supply.usecase.RewardsBalanceTicker.Accrual
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Ticks the currency total while some of its extra balances accrue (a lending position); only those parts grow.
 *
 * Emits nothing when the growth of a tick stays below [MAX_FIAT_DECIMALS] fiat decimals: a small position next to a
 * large balance would otherwise show a long tail of digits on the whole total.
 */
class GetAccruingBalanceUseCase(
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {

    operator fun invoke(status: CryptoCurrencyStatus, appCurrency: AppCurrency): Flow<YieldSupplyRewardBalance> = flow {
        val amount = status.value.amount ?: return@flow
        val accruing = status.value.accruingContributions()
            .map { Accrual(amount = it.totalDeltaCryptoAmount(), rate = it.accrualRate) }
        if (accruing.isEmpty()) return@flow

        val fiatRate = status.value.fiatRate
        if (fiatRate != null && RewardsBalanceTicker.fiatDecimals(accruing, fiatRate) > MAX_FIAT_DECIMALS) return@flow

        val fixedPart = amount + status.value.totalContributionsDeltaOrNull().orZero() - accruing.sumOf(Accrual::amount)
        emitAll(
            RewardsBalanceTicker.tick(
                fixedPart = fixedPart,
                accruing = accruing,
                currency = status.currency,
                fiatRate = fiatRate,
                appCurrency = appCurrency,
            ),
        )
    }.flowOn(dispatcherProvider.default)

    private companion object {
        const val MAX_FIAT_DECIMALS = 6
    }
}