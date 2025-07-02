package com.tangem.features.swap.v2.impl.amount

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountModel
import com.tangem.features.swap.v2.impl.amount.ui.SwapAmountContent
import com.tangem.features.swap.v2.impl.common.SwapNavigationModelCallback
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class SwapAmountComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SwapAmountComponentParams.AmountParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SwapAmountModel = getOrCreateModel(params = params)

    fun updateState(amountUM: SwapAmountUM) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val amountUM by model.uiState.collectAsStateWithLifecycle()

        SwapAmountContent(
            amountUM = amountUM,
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
            clickIntents = model,
        )
    }

    interface ModelCallback : SwapNavigationModelCallback {
        fun onAmountResult(amountUM: SwapAmountUM)
    }
}