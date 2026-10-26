package com.tangem.features.yield.supply.impl.incentives.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.yield.supply.impl.incentives.banner.model.YieldSupplyRewardsBannerModel
import com.tangem.features.yield.supply.impl.incentives.banner.ui.YieldSupplyRewardsBanner
import kotlinx.coroutines.flow.StateFlow

internal class DefaultYieldSupplyRewardsBannerComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyRewardsBannerModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        state?.let { bannerState ->
            YieldSupplyRewardsBanner(
                state = bannerState,
                onClaimClick = params.onClaimClick,
                modifier = modifier,
            )
        }
    }

    internal data class Params(
        val cryptoCurrency: CryptoCurrency.Token,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val onClaimClick: () -> Unit,
    )
}