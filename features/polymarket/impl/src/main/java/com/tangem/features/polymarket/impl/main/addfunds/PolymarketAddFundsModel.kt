package com.tangem.features.polymarket.impl.main.addfunds

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.features.polymarket.impl.main.addfunds.analytics.PredictionAddFundsAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

@Stable
@ModelScoped
internal class PolymarketAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<PolymarketAddFundsComponent.Params>()

    val uiState: PolymarketAddFundsUM = PolymarketAddFundsUM(
        items = persistentListOf(
            PolymarketAddFundsUM.Item(
                icon = Icons.ic_logo_tangem_20,
                title = resourceReference(R.string.prediction_add_funds_swap_title),
                description = resourceReference(R.string.prediction_add_funds_swap_subtitle),
                onClick = ::onSwapClick,
            ),
            PolymarketAddFundsUM.Item(
                icon = Icons.ic_grid_20,
                title = resourceReference(R.string.prediction_add_funds_receive_title),
                description = resourceReference(R.string.prediction_add_funds_receive_subtitle),
                onClick = ::onReceiveClick,
            ),
        ),
        onDismiss = ::onDismiss,
    )

    init {
        analytics.send(PredictionAddFundsAnalyticsEvent.MethodScreenOpened())
    }

    fun onDismiss() {
        params.callback.onDismissAddFunds()
    }

    private fun onSwapClick() {
        analytics.send(PredictionAddFundsAnalyticsEvent.ButtonSwap())
        params.callback.onSwapClick()
    }

    private fun onReceiveClick() {
        analytics.send(PredictionAddFundsAnalyticsEvent.ButtonReceive())
        params.callback.onReceiveClick()
    }
}