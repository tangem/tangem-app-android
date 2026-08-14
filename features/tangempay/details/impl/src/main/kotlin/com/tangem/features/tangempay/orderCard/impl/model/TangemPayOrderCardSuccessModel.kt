package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
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
) : Model() {

    private val params = paramsContainer.require<TangemPayOrderCardSuccessComponent.Params>()

    val state: StateFlow<TangemPayOrderCardSuccessScreenUM>
        field = MutableStateFlow(
            TangemPayOrderCardSuccessScreenUM(
                deliveryEtaMaxBusinessDays = params.deliveryEtaMaxBusinessDays,
                email = params.email,
                onShowCardClick = params.onShowCard,
            ),
        )
}