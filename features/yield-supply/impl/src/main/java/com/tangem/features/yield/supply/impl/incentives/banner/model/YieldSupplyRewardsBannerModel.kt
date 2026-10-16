package com.tangem.features.yield.supply.impl.incentives.banner.model

import arrow.core.Either
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.common.formatter.formatAmount
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.banner.DefaultYieldSupplyRewardsBannerComponent
import com.tangem.features.yield.supply.impl.incentives.banner.entity.YieldSupplyRewardsBannerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

@ModelScoped
internal class YieldSupplyRewardsBannerModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
) : Model() {

    private val params: DefaultYieldSupplyRewardsBannerComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyRewardsBannerUM?>
        field = MutableStateFlow<YieldSupplyRewardsBannerUM?>(null)

    private var isClaimInFlight: Boolean = false
    private var context: BannerContext? = null
    private var rewards: YieldSupplyRewardsBannerUM? = null

    init {
        modelScope.launch {
            val address = params.cryptoCurrencyStatusFlow
                .mapNotNull { it.value.networkAddress?.defaultAddress?.value }
                .first()

            val campaignChainIds = yieldSupplyMarketFlowUseCase(params.cryptoCurrency)
                .map { market -> market?.takeIf { it.campaign?.hasActiveCampaign == true }?.chainId }
                .flowOn(dispatchers.io)
                .shareIn(scope = this, started = SharingStarted.Eagerly, replay = 1)

            // Restarting on every market update would drop the claim observation mid-flight, so only a chain change
            // restarts; a market update of the same chain just reads the rewards again
            campaignChainIds
                .distinctUntilChanged()
                .collectLatest { chainId ->
                    rewards = null
                    if (chainId == null) {
                        context = null
                        uiState.value = null
                        return@collectLatest
                    }

                    context = BannerContext(chainId = chainId, userAddress = address)
                    showRewards(
                        chainId = chainId,
                        userAddress = address,
                        marketUpdates = campaignChainIds.filter { it == chainId },
                    )
                }
        }
    }

    private suspend fun showRewards(chainId: Int, userAddress: String, marketUpdates: Flow<Int?>) = coroutineScope {
        val claimInFlight = yieldSupplyClaimTracker.isClaimInFlight(chainId = chainId, userAddress = userAddress)

        launch {
            claimInFlight.collect { inFlight ->
                isClaimInFlight = inFlight
                publishBanner()
            }
        }

        // The newest trigger wins, so a stale read cannot overwrite a fresher one. A failed read says nothing about
        // the rewards, so whatever is shown stays and the next market update retries
        merge(marketUpdates, claimInFlight.settlements())
            .collectLatest {
                readRewards(chainId = chainId, userAddress = userAddress).onRight { read ->
                    rewards = read
                    publishBanner()
                }
            }
    }

    private fun publishBanner() {
        uiState.value = rewards?.withClaimInFlight(isClaimInFlight)
    }

    private fun Flow<Boolean>.settlements(): Flow<Unit> = flow {
        var wasInFlight = false
        collect { inFlight ->
            if (wasInFlight && !inFlight) emit(Unit)
            wasInFlight = inFlight
        }
    }

    fun onClaimClick() {
        if (isClaimInFlight) return
        val bannerContext = context ?: return
        params.onClaimClick(bannerContext.chainId, bannerContext.userAddress)
    }

    private suspend fun readRewards(
        chainId: Int,
        userAddress: String,
    ): Either<YieldIncentivesError, YieldSupplyRewardsBannerUM?> = withContext(dispatchers.io) {
        getYieldRewardsSummaryUseCase(chainId = chainId, userAddress = userAddress)
            .onLeft(::logHidden)
            .map { summary -> summary.toUM() }
    }

    private fun logHidden(error: YieldIncentivesError) = when (error) {
        YieldIncentivesError.FeatureAbsent ->
            TangemLogger.i("Yield rewards banner hidden, incentives are not configured for this chain")
        YieldIncentivesError.Unavailable ->
            TangemLogger.e("Yield rewards banner hidden, the incentives request failed")
    }

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