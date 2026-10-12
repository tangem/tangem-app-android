package com.tangem.features.commonfeatures.impl.portfolioselector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorBS
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorContentV3
import com.tangem.features.commonfeatures.impl.portfolioselector.ui.PortfolioSelectorModal
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
        val onBack: () -> Unit = { params.bsCallback?.onBack() }
        if (params.settings.isDs3Modal) {
            PortfolioSelectorModal(state = state, onDismiss = ::dismiss, onBack = onBack)
        } else {
            PortfolioSelectorBS(state = state, onDismiss = ::dismiss, onBack = onBack)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val listBottomPadding = PaddingValues(bottom = LocalTangemBottomSheetContentBottomInset.current)

        Column(modifier) {
            PortfolioSelectorContentV3(
                state = state,
                modifier = Modifier,
                contentPadding = listBottomPadding,
            )

            val button = state.button

            if (button != null) {
                SpacerH16()

                SecondaryButton(
                    text = button.text.resolveReference(),
                    onClick = button.onClick,
                    enabled = button.isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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