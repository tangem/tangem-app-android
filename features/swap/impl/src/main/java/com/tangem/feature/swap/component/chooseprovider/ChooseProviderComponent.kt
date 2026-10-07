package com.tangem.feature.swap.component.chooseprovider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.feature.swap.component.chooseprovider.model.ChooseProviderModel
import com.tangem.feature.swap.models.states.ChooseProviderSheetSource
import com.tangem.feature.swap.ui.ChooseProviderContent
import com.tangem.feature.swap.ui.ChooseProviderTitle
import kotlinx.coroutines.flow.StateFlow

internal class ChooseProviderComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by context {

    private val model: ChooseProviderModel = getOrCreateModel(params = params)

    @Composable
    override fun Title() {
        val state by model.uiState.collectAsStateWithLifecycle()
        ChooseProviderTitle(content = state, onCloseClick = params.onCloseClick)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ChooseProviderContent(content = state, modifier = modifier)
    }

    @Composable
    override fun Footer() = Unit

    data class Params(
        val source: StateFlow<ChooseProviderSheetSource>,
        val onCloseClick: () -> Unit,
    )
}