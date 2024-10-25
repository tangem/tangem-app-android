package com.tangem.features.onramp.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.onramp.component.ConfirmResidencyComponent
import com.tangem.features.onramp.model.ConfirmResidencyModel
import com.tangem.features.onramp.ui.ConfirmResidencyBottomSheet
import com.tangem.features.onramp.ui.ConfirmResidencyBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultConfirmResidencyComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: ConfirmResidencyComponent.Params,
) : ConfirmResidencyComponent, AppComponentContext by context {

    private val model: ConfirmResidencyModel = getOrCreateModel(params)

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
        ConfirmResidencyBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                ConfirmResidencyBottomSheetContent(model = state, modifier = modifier)
            },
        )
    }

    @AssistedFactory
    interface Factory : ConfirmResidencyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ConfirmResidencyComponent.Params,
        ): DefaultConfirmResidencyComponent
    }
}
