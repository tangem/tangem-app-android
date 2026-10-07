package com.tangem.features.yield.supply.impl.incentives.banner.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.uncapped
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.incentives.banner.DefaultYieldSupplyRewardsBannerComponent
import com.tangem.features.yield.supply.impl.incentives.banner.entity.YieldSupplyRewardsBannerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

@ModelScoped
internal class YieldSupplyRewardsBannerModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
) : Model() {

    private val params: DefaultYieldSupplyRewardsBannerComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyRewardsBannerUM?>
        field = MutableStateFlow<YieldSupplyRewardsBannerUM?>(null)

    init {
        modelScope.launch(dispatchers.io) {
            val userAddress = params.cryptoCurrencyStatusFlow
                .mapNotNull { it.value.networkAddress?.defaultAddress?.value }
                .first()

            yieldSupplyMarketFlowUseCase(params.cryptoCurrency).collectLatest { market ->
                val banner = resolveBanner(market = market, userAddress = userAddress)
                uiState.update { banner }
            }
        }
    }

    private suspend fun resolveBanner(market: YieldMarketToken?, userAddress: String): YieldSupplyRewardsBannerUM? {
        if (market?.campaign?.hasActiveCampaign != true) return null

        return getYieldRewardsSummaryUseCase(chainId = market.chainId, userAddress = userAddress)
            .fold(
                ifLeft = { error ->
                    TangemLogger.e("Yield rewards banner hidden, incentives unavailable: $error")
                    null
                },
                ifRight = { summary -> summary.toUM() },
            )
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

    /** The backend estimates rewards in USD whatever currency the app displays, so the symbol is not the app's. */
    private fun BigDecimal.formatUsd(): String = format {
        fiat(fiatCurrencyCode = USD_CODE, fiatCurrencySymbol = USD_SYMBOL)
    }

    /** Uncapped: the default crypto format stops at 8 decimals, which renders a dust reward as a plain zero. */
    private fun List<YieldReward>.formatAmounts(): String = joinToString { reward ->
        reward.pendingAmount.toBigDecimal().movePointLeft(reward.tokenDecimals).format {
            crypto(symbol = reward.symbol, decimals = reward.tokenDecimals).uncapped()
        }
    }

    private companion object {
        const val USD_CODE = "USD"
        const val USD_SYMBOL = "$"
    }
}