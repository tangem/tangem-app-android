package com.tangem.features.commonfeatures.impl.portfolioselector

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorBS
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorContent
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorContentV2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DefaultPortfolioSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PortfolioSelectorComponent.Params,
) : AppComponentContext by appComponentContext, PortfolioSelectorComponent {

    private val model: PortfolioSelectorModel = getOrCreateModel(params)

    override val title: StateFlow<TextReference>
        get() = model.state
            .map { it.title }
            .stateIn(componentScope, SharingStarted.Lazily, model.state.value.title)

    override fun dismiss() {
        params.bsCallback?.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        PortfolioSelectorBS(state = state, onDismiss = ::dismiss, onBack = { params.bsCallback?.onBack() })
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val listBottomPadding = PaddingValues(bottom = LocalTangemBottomSheetContentBottomInset.current)
        if (LocalRedesignEnabled.current) {
            PortfolioSelectorContentV2(
                state = state,
                modifier = modifier,
                contentPadding = listBottomPadding,
            )
        } else {
            PortfolioSelectorContent(
                state = state,
                modifier = modifier,
                contentPadding = listBottomPadding,
            )
        }
    }

    @AssistedFactory
    interface Factory : PortfolioSelectorComponent.Factory {
        override fun create(
            appComponentContext: AppComponentContext,
            params: PortfolioSelectorComponent.Params,
        ): DefaultPortfolioSelectorComponent
    }
}