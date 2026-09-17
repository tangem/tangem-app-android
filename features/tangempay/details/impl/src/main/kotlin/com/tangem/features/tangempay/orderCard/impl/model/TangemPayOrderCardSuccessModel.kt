package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardIntent
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardSuccessComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardSuccessScreenUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayOrderCardSuccessModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayOrderCardSuccessComponent.Params>()

    val state: StateFlow<TangemPayOrderCardSuccessScreenUM>
        field = MutableStateFlow(
            TangemPayOrderCardSuccessScreenUM(
                deliveryEtaMaxBusinessDays = params.deliveryEtaMaxBusinessDays,
                email = params.email,
                buttonText = resourceReference(
                    when (params.intent) {
                        TangemPayOrderCardIntent.Issue -> R.string.tangempay_order_success_show_card
                        is TangemPayOrderCardIntent.ReissuePlastic -> R.string.common_close
                    },
                ),
                onFinishClick = params.onFinish,
            ),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.Plastic.CardOrderedSuccessScreenShowed())
    }
}