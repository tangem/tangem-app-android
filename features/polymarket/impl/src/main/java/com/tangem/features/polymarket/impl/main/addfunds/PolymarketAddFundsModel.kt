package com.tangem.features.polymarket.impl.main.addfunds

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.resourceReference
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
                iconRes = CoreUiR.drawable.ic_exchange_horizontal_24,
                title = resourceReference(R.string.common_swap),
                description = resourceReference(R.string.prediction_add_funds_swap_subtitle),
                onClick = ::onSwapClick,
            ),
            PolymarketAddFundsUM.Item(
                iconRes = CoreUiR.drawable.ic_arrow_down_24,
                title = resourceReference(R.string.common_receive),
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