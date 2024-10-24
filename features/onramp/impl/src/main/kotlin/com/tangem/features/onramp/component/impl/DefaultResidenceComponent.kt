package com.tangem.features.onramp.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.onramp.component.ResidenceComponent
import com.tangem.features.onramp.model.ResidenceModel
import com.tangem.features.onramp.ui.ResidenceBottomSheet
import com.tangem.features.onramp.ui.ResidenceBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultResidenceComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: ResidenceComponent.Params,
) : ResidenceComponent, AppComponentContext by context {

    private val model: ResidenceModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheetConfig = remember {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        ResidenceBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                ResidenceBottomSheetContent(model = state, modifier = modifier)
            },
        )
    }

    @AssistedFactory
    interface Factory : ResidenceComponent.Factory {
        override fun create(context: AppComponentContext, params: ResidenceComponent.Params): DefaultResidenceComponent
    }
}
