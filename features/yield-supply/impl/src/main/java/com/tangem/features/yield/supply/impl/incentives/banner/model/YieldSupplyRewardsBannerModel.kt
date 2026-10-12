package com.tangem.features.yield.supply.impl.incentives.banner.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.INCENTIVES_CACHE_SECONDS
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.RefreshYieldIncentivesUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.common.formatter.formatAmount
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.banner.DefaultYieldSupplyRewardsBannerComponent
import com.tangem.features.yield.supply.impl.incentives.banner.entity.YieldSupplyRewardsBannerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.tangem.core.res.R as CoreResR

@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class YieldSupplyRewardsBannerModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val refreshYieldIncentivesUseCase: RefreshYieldIncentivesUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
) : Model() {

    private val params: DefaultYieldSupplyRewardsBannerComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyRewardsBannerUM?>
        field = MutableStateFlow<YieldSupplyRewardsBannerUM?>(null)

    private var market: YieldMarketToken? = null
    private var userAddress: String? = null
    private var isClaimInFlight: Boolean = false
    private var context: BannerContext? = null

    /** What the rewards themselves say, before the claim state is laid over them. */
    private var rewards: YieldSupplyRewardsBannerUM? = null

    /** The market collector and the claim outcome both publish; interleaving them would show a half-read banner. */
    private val publishMutex = Mutex()

    init {
        modelScope.launch {
            val address = params.cryptoCurrencyStatusFlow
                .mapNotNull { it.value.networkAddress?.defaultAddress?.value }
                .first()
            userAddress = address

            // The claim belongs to the module, so the chain the market names is what the in-flight state is read
            // for: a claim started from another token of the same module disables this banner's button too.
            yieldSupplyMarketFlowUseCase(params.cryptoCurrency)
                .flatMapLatest { currentMarket ->
                    val chainId = currentMarket?.chainId ?: return@flatMapLatest flowOf(currentMarket to false)

                    yieldSupplyClaimTracker
                        .isClaimInFlight(chainId = chainId, userAddress = address)
                        .map { currentClaimInFlight -> currentMarket to currentClaimInFlight }
                }
                .flowOn(dispatchers.io)
                .collectLatest { (currentMarket, currentClaimInFlight) ->
                    market = currentMarket
                    isClaimInFlight = currentClaimInFlight
                    publishBanner()
                }
        }
        subscribeOnClaimOutcome()
    }

    /**
     * Merkl needs a few minutes to process a settled claim, so the first read after confirmation may still carry
     * the rewards the user has just claimed. One further read is scheduled past the cache window for that case —
     * a poll would hammer an endpoint whose own window is the same length.
     */
    private fun subscribeOnClaimOutcome() {
        yieldSupplyClaimTracker.outcomes
            .filter { claimOutcome ->
                val bannerContext = context

                bannerContext != null &&
                    claimOutcome.chainId == bannerContext.chainId &&
                    claimOutcome.userAddress.equals(bannerContext.userAddress, ignoreCase = true) &&
                    claimOutcome.outcome == YieldSupplyTransactionOutcome.Confirmed
            }
            .onEach { _ ->
                // Compared without the claim-state overlay on purpose: the tracker clears the in-flight flag just
                // before it reports the outcome, so the CTA flips at the same moment and would read as a change.
                val before = rewards
                val refreshed = publishBanner()
                if (refreshed != before) return@onEach

                delay(INCENTIVES_CACHE_SECONDS.seconds)
                context?.let { bannerContext ->
                    refreshYieldIncentivesUseCase(
                        chainId = bannerContext.chainId,
                        userAddress = bannerContext.userAddress,
                    )
                }
                publishBanner()
            }
            .launchIn(modelScope)
    }

    private suspend fun publishBanner(): YieldSupplyRewardsBannerUM? = publishMutex.withLock {
        val address = userAddress ?: return null
        val banner = withContext(dispatchers.io) { resolveBanner(market = market, userAddress = address) }
        rewards = banner
        uiState.update { banner?.withClaimInFlight(isClaimInFlight) }

        banner
    }

    private suspend fun resolveBanner(market: YieldMarketToken?, userAddress: String): YieldSupplyRewardsBannerUM? {
        if (market?.campaign?.hasActiveCampaign != true) return null
        context = BannerContext(chainId = market.chainId, userAddress = userAddress)

        return getYieldRewardsSummaryUseCase(chainId = market.chainId, userAddress = userAddress)
            .fold(
                ifLeft = { error ->
                    logHidden(error)
                    null
                },
                ifRight = { summary -> summary.toUM() },
            )
    }

    private fun logHidden(error: YieldIncentivesError) = when (error) {
        YieldIncentivesError.FeatureAbsent ->
            TangemLogger.i("Yield rewards banner hidden, incentives are not configured for this chain")
        YieldIncentivesError.Unavailable ->
            TangemLogger.e("Yield rewards banner hidden, the incentives request failed")
    }

    /** A claim the chain has not answered for yet: the rewards are still shown, but they cannot be claimed twice. */
    private fun YieldSupplyRewardsBannerUM.withClaimInFlight(isClaimInFlight: Boolean) = when {
        !isClaimInFlight -> this
        claim == YieldSupplyRewardsBannerUM.Claim.Hidden -> this
        else -> copy(claim = YieldSupplyRewardsBannerUM.Claim.Disabled)
    }

    private fun YieldRewardsSummary.toUM(): YieldSupplyRewardsBannerUM? = when (this) {
        is YieldRewardsSummary.None -> null
        is YieldRewardsSummary.PendingOnly -> YieldSupplyRewardsBannerUM(
            title = resourceReference(CoreResR.string.yield_module_rewards_banner_pending_title),
            description = resourceReference(
                id = CoreResR.string.yield_module_rewards_banner_pending_subtitle,
                formatArgs = wrappedList(pending.formatAmounts()),
            ),
            claim = YieldSupplyRewardsBannerUM.Claim.Hidden,
        )
        is YieldRewardsSummary.Deferred -> claimableUM(
            totalNetUsd = totalNetUsd,
            descriptionResId = CoreResR.string.yield_module_rewards_banner_below_threshold_subtitle,
            claim = YieldSupplyRewardsBannerUM.Claim.Disabled,
        )
        is YieldRewardsSummary.Claimable -> claimableUM(
            totalNetUsd = totalNetUsd,
            descriptionResId = CoreResR.string.yield_module_rewards_banner_subtitle,
            claim = YieldSupplyRewardsBannerUM.Claim.Enabled,
        )
    }

    private fun claimableUM(totalNetUsd: BigDecimal, descriptionResId: Int, claim: YieldSupplyRewardsBannerUM.Claim) =
        YieldSupplyRewardsBannerUM(
            title = resourceReference(
                id = CoreResR.string.yield_module_rewards_banner_title,
                formatArgs = wrappedList(totalNetUsd.formatUsd()),
            ),
            description = resourceReference(descriptionResId),
            claim = claim,
        )

    private fun List<YieldReward>.formatAmounts(): String = joinToString { reward ->
        reward.formatAmount(reward.pendingAmount)
    }

    private data class BannerContext(val chainId: Int, val userAddress: String)
}