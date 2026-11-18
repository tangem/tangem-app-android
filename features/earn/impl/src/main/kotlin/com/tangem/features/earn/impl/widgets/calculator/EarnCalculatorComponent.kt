package com.tangem.features.earn.impl.widgets.calculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.widgets.calculator.model.EarnCalculatorModel
import com.tangem.features.earn.impl.widgets.calculator.ui.EarnCalculatorContent
import com.tangem.features.earn.product.api.product.EarnProjection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The host marks the calculator, or the content behind it, with `Modifier.hazeSourceTangem()`: otherwise the horizon
 * menu has nothing to blur.
 */
internal class EarnCalculatorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: EarnCalculatorModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        state?.let { EarnCalculatorContent(state = it, modifier = modifier) }
    }

    enum class Input { InitialBalance, MonthlyTopUp }

    data class Params(
        val projection: Flow<EarnProjection?>,
        val incentiveRate: Flow<EarnProductRate?> = flowOf(null),
        val callback: ModelCallback,
    )

    interface ModelCallback {

        fun onIncentiveClick()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnCalculatorComponent
    }

    companion object {
        const val KEY = "calculator"
    }
}