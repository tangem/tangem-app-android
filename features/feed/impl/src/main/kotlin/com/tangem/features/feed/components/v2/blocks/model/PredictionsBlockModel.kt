package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class PredictionsBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val featureToggles: PolymarketFeatureToggles,
    private val abTestsManager: ABTestsManager,
) : Model() {

    val isVisible: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        if (featureToggles.isPolymarketEnabled) {
            modelScope.launch {
                val variant = abTestsManager.getValue(key = EXPERIMENT_KEY, defaultValue = OFF_VARIANT)
                isVisible.value = variant == ON_VARIANT
            }
        }
    }

    private companion object {
        const val EXPERIMENT_KEY = "twi_1576_polymarket"
        const val ON_VARIANT = "on"
        const val OFF_VARIANT = "off"
    }
}