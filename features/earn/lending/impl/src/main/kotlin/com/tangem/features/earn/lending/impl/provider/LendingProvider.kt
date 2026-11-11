package com.tangem.features.earn.lending.impl.provider

import arrow.core.Either
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProvider
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * The external service Lending runs on. Everything outside the provider's own package knows only this boundary, so a
 * second provider plugs in without touching the product.
 */
internal interface LendingProvider {

    val info: EarnProvider

    /** Chart periods: the depth of the provider's rate history defines them */
    val apyPeriods: List<EarnPeriod>

    /** Share of the earned yield the provider keeps */
    val performanceFee: BigDecimal

    val isAvailable: Boolean

    /** @return null when Lending is not offered for [currency] (a token, another network, a joint account) */
    fun resolveAsset(currency: CryptoCurrency): LendingAsset?

    /** The first item comes at once, the next ones when the data it depends on changes */
    fun market(userWalletId: UserWalletId, asset: LendingAsset): Flow<LendingMarket>

    suspend fun refresh(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit>

    suspend fun minDeposit(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, BigDecimal>

    /** An empty history means "no data"; a backend or network failure goes to `Left` */
    suspend fun apyHistory(asset: LendingAsset, period: EarnPeriod): Either<Throwable, EarnApyHistory>
}

/**
 * @property wrappedToken the token the provider holds a coin as; its balance may already include the position. Null when
 * the provider holds [currency] as is
 */
internal data class LendingAsset(
    val currency: CryptoCurrency,
    val wrappedToken: CryptoCurrency.Token?,
)

/**
 * @property rate          rate with the provider's incentives; kept for a closed market since its position is visible
 * @property isOpen        the provider accepts deposits of the asset; a closed market does not hide the position
 * @property maxWithdraw   the position capped by the provider's liquidity; null while there is no position
 * @property providerState read only by the provider's own transaction builder; null until read
 */
internal data class LendingMarket(
    val rate: EarnProductRate?,
    val isOpen: Boolean,
    val deposit: LendingDepositGate,
    val position: LendingPosition?,
    val maxWithdraw: BigDecimal?,
    val providerState: LendingProviderState?,
    val source: StatusSource,
)

internal data class LendingPosition(val cryptoAmount: BigDecimal)

internal interface LendingProviderState {

    /** Fingerprint of what the transactions depend on: when it changes, a prepared plan is stale */
    val planKey: String
}

internal sealed interface LendingDepositGate {

    data object Loading : LendingDepositGate

    data object ReadFailed : LendingDepositGate

    data object Open : LendingDepositGate

    /** The provider takes no new money; withdrawals stay available */
    data object Unavailable : LendingDepositGate

    val isDepositBlocked: Boolean
        get() = this == Unavailable
}