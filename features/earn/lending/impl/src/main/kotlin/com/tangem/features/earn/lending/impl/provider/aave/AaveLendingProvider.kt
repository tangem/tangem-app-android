package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.R
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.models.status.findFor
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.lending.impl.provider.LendingPosition
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnLegal
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Lending on top of the Yield Module, where a coin is held in Aave as its wrapped token */
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class AaveLendingProvider @Inject constructor(
    private val assetResolver: AaveAssetResolver,
    private val markets: AaveMarkets,
    private val moduleReader: AaveModuleReader,
    private val apyHistoryLoader: AaveApyHistoryLoader,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val networkStatusSupplier: SingleNetworkStatusSupplier,
    private val statusSupplier: YieldModuleStatusSupplier,
    private val statusFetcher: YieldModuleStatusFetcher,
) : LendingProvider {

    override val info = EarnProvider(title = stringReference("Aave"), icon = ImageReference.Res(R.drawable.img_aave_22))

    override val websiteUrl: String = "https://aave.com"

    override val legal = EarnLegal(
        termsOfUseUrl = "https://aave.com/terms-of-service",
        privacyPolicyUrl = "https://aave.com/privacy-policy",
    )

    override val apyPeriods: List<EarnPeriod> = AaveApyPeriods.All

    /** A placeholder from the design until the product confirms the real fee */
    override val performanceFee: BigDecimal = BigDecimal("0.15")

    override val isAvailable: Boolean = true

    override fun resolveAsset(currency: CryptoCurrency): LendingAsset? = assetResolver.resolve(currency)

    override fun market(userWalletId: UserWalletId, asset: LendingAsset): Flow<LendingMarket> = channelFlow {
        val statusFetch = MutableStateFlow(StatusFetch.Pending)

        val isSupported = yieldSupplyRepository.isYieldSupplySupported(userWalletId, asset.currency)

        // Re-read on every network update of the wallet rather than on a timer. The wallet refresh reads the reserve
        // token only when it is in the portfolio, so Lending asks for its status itself
        val onChain = networkStatusSupplier(SingleNetworkStatusProducer.Params(userWalletId, asset.currency.network))
            .mapLatest { networkStatus ->
                coroutineScope {
                    launch {
                        val isFailed = fetchStatus(userWalletId, asset).isLeft()
                        statusFetch.value = when {
                            isFailed -> StatusFetch.Failed
                            // Without a derivation the fetcher has no wallet manager and stores no status
                            networkStatus.value is NetworkStatus.MissedDerivation -> StatusFetch.NoWallet
                            else -> StatusFetch.Pending
                        }
                    }
                    moduleReader.read(userWalletId, asset)
                }
            }
            .scan(OnChainState.Initial, OnChainState::next)

        combine(
            flow = markets.observeRead(asset),
            flow2 = onChain,
            flow3 = observeStatus(userWalletId, asset),
            flow4 = statusFetch,
        ) { market, chain, status, fetch ->
            toMarket(
                market = market,
                chain = chain,
                status = StatusRead(status = status, fetch = fetch),
                isSupported = isSupported,
            )
        }.collect(::send)
    }

    override suspend fun refresh(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit> = either {
        markets.update().bind()
        fetchStatus(userWalletId, asset).bind()
    }

    override suspend fun refreshPosition(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit> =
        fetchStatus(userWalletId, asset)

    override suspend fun apyHistory(asset: LendingAsset, period: EarnPeriod): Either<Throwable, EarnApyHistory> =
        apyHistoryLoader.load(asset, period)

    private suspend fun fetchStatus(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, Unit> =
        statusFetcher(YieldModuleStatusFetcher.Params(userWalletId = userWalletId, tokens = setOf(asset.reserveToken)))
            .onLeft { TangemLogger.e("Aave reserve status fetch failed", it) }

    private fun observeStatus(userWalletId: UserWalletId, asset: LendingAsset): Flow<YieldModuleStatus?> =
        statusSupplier(YieldModuleStatusProducer.Params(userWalletId))
            .map { statuses -> statuses.findFor(asset.reserveToken) }
            .onStart { emit(null) }
            .distinctUntilChanged()

    private fun toMarket(
        market: AaveMarketRead,
        chain: OnChainState,
        status: StatusRead,
        isSupported: Boolean,
    ): LendingMarket {
        val read = chain.read?.let { AaveModuleRead(status.status, it.version, it.withdrawLiquidity) }
        val position = status.status?.position
        // Without the pool liquidity a withdrawal is capped by the position only
        val liquidity = read?.withdrawLiquidity

        return LendingMarket(
            rate = market.market?.effectiveApy?.percentToApy(),
            // The market's isActive is the Yield Mode switch of WETH, which the backend keeps off while Lending owns it
            isOpen = isSupported,
            deposit = read?.let { status.depositGate(it.toState()) } ?: chain.unreadGate(),
            position = position?.let(::LendingPosition),
            maxWithdraw = if (position != null && liquidity != null) position.min(liquidity) else position,
            providerState = read,
            source = listOf(chain.source, status.source).getResultStatusSource(),
            rateSource = market.source,
            isPositionLoading = status.isPositionLoading,
        )
    }

    /**
     * A failed fetch leaves no trace in the status store for a token it has never held, so without [fetch] a missing
     * reserve status would look like a status still on its way
     */
    private data class StatusRead(val status: YieldModuleStatus?, val fetch: StatusFetch) {

        private val isFetchFailed: Boolean
            get() = fetch == StatusFetch.Failed

        val source: StatusSource
            get() = status?.source ?: if (isFetchFailed) StatusSource.ONLY_CACHE else StatusSource.CACHE

        val isPositionLoading: Boolean
            get() = status?.source == StatusSource.CACHE || status == null && fetch == StatusFetch.Pending

        fun depositGate(state: AaveModuleState): LendingDepositGate =
            if (state is AaveModuleState.StatusUnknown && isFetchFailed) {
                LendingDepositGate.ReadFailed
            } else {
                state.toDepositGate()
            }
    }

    /** The last successful read survives a failure, only its freshness changes */
    private data class OnChainState(val read: AaveOnChainRead?, val source: StatusSource) {

        fun next(result: Either<Throwable, AaveOnChainRead>): OnChainState = result.fold(
            ifLeft = { copy(source = StatusSource.ONLY_CACHE) },
            ifRight = { OnChainState(read = it, source = StatusSource.ACTUAL) },
        )

        fun unreadGate(): LendingDepositGate =
            if (source == StatusSource.ONLY_CACHE) LendingDepositGate.ReadFailed else LendingDepositGate.Loading

        companion object {
            val Initial = OnChainState(read = null, source = StatusSource.CACHE)
        }
    }

    private enum class StatusFetch { Pending, Failed, NoWallet }
}