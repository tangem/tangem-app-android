package com.tangem.features.send.v2.subcomponents.fee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.subcomponents.fee.ui.FeeBlock
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SendFeeBlockComponent(
    appComponentContext: AppComponentContext,
    private val params: SendFeeComponentParams.FeeBlockParams,
    val onResult: (FeeUM) -> Unit,
    val onClick: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendFeeModel = getOrCreateModel(params = params)

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    fun updateState(state: FeeUM) = model.updateState(state)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()
        val isClickEnabled = params.blockClickEnableFlow.collectAsStateWithLifecycle()
        FeeBlock(
            feeUM = state.value,
            isClickEnabled = isClickEnabled.value,
            onClick = onClick,
        )
    }
}