package com.tangem.features.yield.supply.impl.promo.v2.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
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

    init {
        load()
    }

    private fun load() {
        val token = params.currency as? CryptoCurrency.Token ?: return

        modelScope.launch(dispatchers.io) {
            val rewardsApr = yieldSupplyGetTokenStatusUseCase(token).getOrNull()
                ?.campaign
                ?.takeIf { it.hasActiveCampaign }
                ?.rewardsApr
                ?.takeIf { it > BigDecimal.ZERO }

            uiState.update { it.copy(incentiveChip = incentiveChipOf(rewardsApr)) }
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

    private fun incentiveChipOf(rewardsApr: BigDecimal?) = rewardsApr?.let { apr ->
        resourceReference(
            id = R.string.yield_module_promo_screen_incentivised_chip,
            formatArgs = wrappedList("${apr.stripTrailingZeros().toPlainString()}%"),
        )
    }

    private companion object {
        const val AAVE_TOS_URL = "https://aave.com/terms-of-service"
        const val AAVE_PRIVACY_URL = "https://aave.com/privacy-policy"
    }
}