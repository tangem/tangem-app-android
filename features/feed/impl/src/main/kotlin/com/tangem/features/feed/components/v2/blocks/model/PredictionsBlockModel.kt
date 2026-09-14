package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Whether Predictions is shipped at all — the block has nothing else to decide yet. */
@Stable
@ModelScoped
internal class PredictionsBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    featureToggles: PolymarketFeatureToggles,
) : Model() {

    val isVisible: StateFlow<Boolean> = MutableStateFlow(featureToggles.isPolymarketEnabled)
}