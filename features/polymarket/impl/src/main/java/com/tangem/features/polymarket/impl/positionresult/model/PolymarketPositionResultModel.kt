package com.tangem.features.polymarket.impl.positionresult.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.polymarket.impl.positionresult.PolymarketPositionResultComponent
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class PolymarketPositionResultModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketPositionResultComponent.Params>()

    val uiState: StateFlow<PolymarketPositionResultUM>
        field = MutableStateFlow(PolymarketPositionResultConverter().convert(params.position))
}