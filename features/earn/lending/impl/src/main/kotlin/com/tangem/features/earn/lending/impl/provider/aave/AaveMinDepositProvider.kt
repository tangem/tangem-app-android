package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The threshold follows the Yield Mode rule: the network fee is at most a share of the deposit. It is computed from the
 * network fee, so it is cached for a minute instead of being re-read on every snapshot.
 *
 * The ETH status is read from the wallet status list here and not in the supplier: the balance contribution reads the
 * supplier, and the status list includes the contributions, which would close a loop.
 */
@Singleton
internal class AaveMinDepositProvider(
    private val minAmountUseCase: YieldSupplyMinAmountUseCase,
    private val accountStatusListSupplier: SingleAccountStatusListSupplier,
    private val timeSource: TimeSource,
) {

    private val cache = ConcurrentHashMap<CryptoCurrency.ID, Cached>()

    @Inject
    constructor(
        minAmountUseCase: YieldSupplyMinAmountUseCase,
        accountStatusListSupplier: SingleAccountStatusListSupplier,
    ) : this(minAmountUseCase, accountStatusListSupplier, TimeSource.Monotonic)

    suspend fun get(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, BigDecimal> {
        val id = asset.currency.id
        cache[id]?.takeIf { it.mark.elapsedNow() < TTL }?.let { return it.value.right() }

        return either {
            val status = accountStatusListSupplier.getSyncOrNull(userWalletId)
                .getCryptoCurrencyStatus(asset.currency)
                .getOrNull()
                ?: raise(IllegalStateException("No status for $id"))

            minAmountUseCase(userWalletId, status).bind()
        }.onRight { cache[id] = Cached(value = it, mark = timeSource.markNow()) }
    }

    private data class Cached(val value: BigDecimal, val mark: TimeMark)

    private companion object {
        val TTL = 1.minutes
    }
}