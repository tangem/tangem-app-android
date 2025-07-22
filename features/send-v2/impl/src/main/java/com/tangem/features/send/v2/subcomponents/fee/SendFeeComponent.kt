package com.tangem.features.send.v2.subcomponents.fee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeModel
import com.tangem.features.send.v2.subcomponents.fee.ui.SendFeeContent
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

internal class SendFeeComponent(
    appComponentContext: AppComponentContext,
    params: SendFeeComponentParams.FeeParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendFeeModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()

        SendFeeContent(state = state.value, clickIntents = model)
    }

    interface ModelCallback : NavigationModelCallback {
        fun onFeeResult(feeUM: FeeUM)
    }
}