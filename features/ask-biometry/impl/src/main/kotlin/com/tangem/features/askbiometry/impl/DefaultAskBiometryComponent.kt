package com.tangem.features.askbiometry.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.askbiometry.AskBiometryComponent
import com.tangem.features.askbiometry.impl.model.AskBiometryModel
import com.tangem.features.askbiometry.impl.ui.AskBiometry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAskBiometryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AskBiometryComponent.Params,
) : AskBiometryComponent, AppComponentContext by appComponentContext {

    private val model: AskBiometryModel = getOrCreateModel(params)

    override fun dismiss() = model.dismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bsConfig = remember(this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = bsConfig,
            content = { AskBiometry(state) },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        AskBiometry(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory : AskBiometryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AskBiometryComponent.Params,
        ): DefaultAskBiometryComponent
    }
}