package com.tangem.feature.swap.component.slippage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.swap.component.slippage.model.SwapSlippageModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.SwapSlippageContent
import com.tangem.feature.swap.ui.SwapSlippageFooter
import java.math.BigDecimal

internal class SwapSlippageComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by context {

    private val model: SwapSlippageModel = getOrCreateModel(params = params)

    @Composable
    override fun Title() {
        TangemModal.TopNavigation(
            title = resourceReference(R.string.common_slippage),
            onClose = params.onCloseClick,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        SwapSlippageContent(state = state, modifier = modifier)
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        SwapSlippageFooter(state = state)
    }

    data class Params(
        val provider: SwapProvider,
        val defaultPercent: BigDecimal,
        val customSlippage: BigDecimal?,
        val onConfirm: (BigDecimal?) -> Unit,
        val onCloseClick: () -> Unit,
    )
}