package com.tangem.features.earn.lending.impl.provider

import arrow.core.Either
import arrow.core.right
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

/** A provider for product-layer tests: assets and market data are set by the test, nothing is read from a network */
internal class FakeLendingProvider(
    private val assets: Map<CryptoCurrency.ID, LendingAsset> = emptyMap(),
    override val isAvailable: Boolean = true,
) : LendingProvider {

    val marketFlow = MutableStateFlow(loadingMarket())
    var refreshResult: Either<Throwable, Unit> = Unit.right()
    val refreshCalls = mutableListOf<Pair<UserWalletId, LendingAsset>>()
    var refreshPositionResult: Either<Throwable, Unit> = Unit.right()
    val refreshPositionCalls = mutableListOf<Pair<UserWalletId, LendingAsset>>()

    override val info = EarnProvider(title = stringReference("Fake"), icon = ImageReference.Res(0))
    override val apyPeriods: List<EarnPeriod> = emptyList()
    override val performanceFee: BigDecimal = BigDecimal.ZERO

    override fun resolveAsset(currency: CryptoCurrency): LendingAsset? = assets[currency.id]

    override fun market(userWalletId: UserWalletId, asset: LendingAsset): Flow<LendingMarket> = marketFlow

    override suspend fun refresh(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit> {
        refreshCalls += userWalletId to asset
        return refreshResult
    }

    override suspend fun refreshPosition(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit> {
        refreshPositionCalls += userWalletId to asset
        return refreshPositionResult
    }

    override suspend fun minDeposit(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, BigDecimal> =
        BigDecimal.ZERO.right()

    override suspend fun apyHistory(asset: LendingAsset, period: EarnPeriod): Either<Throwable, EarnApyHistory> =
        EarnApyHistory(points = emptyList(), average = null).right()

    companion object {

        fun loadingMarket(source: StatusSource = StatusSource.CACHE) = LendingMarket(
            rate = null,
            isOpen = false,
            deposit = LendingDepositGate.Loading,
            position = null,
            maxWithdraw = null,
            providerState = null,
            source = source,
        )
    }
}