package com.tangem.features.polymarket.impl.positionresult.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.polymarket.impl.positionresult.PolymarketPositionResultStubData
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class PolymarketPositionResultModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<PolymarketPositionResultUM>
        field = MutableStateFlow(PolymarketPositionResultStubData.won)
}