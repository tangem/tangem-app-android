package com.tangem.features.biometry.impl

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.biometry.impl.model.AskBiometryModel
import com.tangem.features.biometry.impl.ui.AskBiometry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DefaultAskBiometryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AskBiometryComponent.Params,
) : AskBiometryComponent, AppComponentContext by appComponentContext {

    private val model: AskBiometryModel = getOrCreateModel(params)
    private val bsShown = instanceKeeper.getOrCreateSimple { MutableStateFlow(true) }

    init {
        model.dismissBSFlow
            .onEach { bsShown.value = false }
            .launchIn(componentScope)
    }

    override fun dismiss() = model.dismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bsShown by bsShown.collectAsStateWithLifecycle()
        val bsConfig = remember(this, bsShown) {
            TangemBottomSheetConfig(
                isShown = bsShown,
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
            modifier = modifier.navigationBarsPadding(),
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