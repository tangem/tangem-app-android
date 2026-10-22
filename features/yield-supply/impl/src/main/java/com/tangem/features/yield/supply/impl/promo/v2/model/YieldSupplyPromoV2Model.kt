package com.tangem.features.yield.supply.impl.promo.v2.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import com.tangem.features.yield.supply.impl.common.formatter.formatApyValue
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promo.v2.entity.YieldSupplyPromoV2UM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** State of the promo screen. The campaign only adds the reward chip to it; the screen itself never waits on one. */
@ModelScoped
internal class YieldSupplyPromoV2Model @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
) : Model() {

    private val params: YieldSupplyPromoComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyPromoV2UM>
        field = MutableStateFlow(buildUiState())

    /**
     * The plain market rate, which this screen never shows: here [YieldSupplyPromoComponent.Params.apy] is the
     * incentivised total, so the variable-rate sheet takes its own rate from the market the chip came from.
     */
    val plainApy: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    init {
        // A boosted wallet is shown the previous screen instead, and nothing here reaches it.
        if (!params.isPromoEnabled) load()
    }

    private fun load() {
        val token = params.currency as? CryptoCurrency.Token ?: return

        modelScope.launch(dispatchers.io) {
            val market = yieldSupplyGetTokenStatusUseCase(token).getOrNull()
            val rewardsApy = market
                ?.campaign
                ?.takeIf { it.hasActiveCampaign }
                ?.rewardsApy
                ?.takeIf { it > BigDecimal.ZERO }

            plainApy.update { market?.apy?.toPlainString() }
            uiState.update { it.copy(incentiveChip = incentiveChipOf(rewardsApy)) }
        }
    }

    private fun buildUiState() = YieldSupplyPromoV2UM(
        title = resourceReference(
            R.string.yield_module_promo_screen_title_v2,
            wrappedList(params.apy.formatApyValue()),
        ),
        rate = resourceReference(R.string.yield_module_promo_screen_variable_rate_info_v2),
        incentiveChip = null,
        tokenSymbol = params.currency.symbol,
        tosLink = AAVE_TOS_URL,
        policyLink = AAVE_PRIVACY_URL,
    )

    private fun incentiveChipOf(rewardsApy: BigDecimal?) = rewardsApy?.let { apy ->
        resourceReference(
            id = R.string.yield_module_promo_screen_incentivised_chip,
            formatArgs = wrappedList(apy.formatApy()),
        )
    }

    private companion object {
        const val AAVE_TOS_URL = "https://aave.com/terms-of-service"
        const val AAVE_PRIVACY_URL = "https://aave.com/privacy-policy"
    }
}