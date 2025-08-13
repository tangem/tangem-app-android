package com.tangem.features.send.v2.send.success

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.send.success.model.SendConfirmSuccessModel
import com.tangem.features.send.v2.send.success.ui.SendConfirmSuccessContent
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeBlockComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SendConfirmSuccessComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendConfirmSuccessModel = getOrCreateModel(params = params)
    private val destinationBlockComponent: SendDestinationBlockComponent = params.destinationBlockComponent
    private val feeBlockComponent: SendFeeBlockComponent = params.feeBlockComponent

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsState()
        SendConfirmSuccessContent(
            sendUM = state,
            destinationBlockComponent = destinationBlockComponent,
            feeBlockComponent = feeBlockComponent,
        )
    }

    data class Params(
        val sendUMFlow: StateFlow<SendUM>,
        val destinationBlockComponent: SendDestinationBlockComponent,
        val feeBlockComponent: SendFeeBlockComponent,
        val analyticsCategoryName: String,
        val currentRoute: Flow<CommonSendRoute>,
        val txUrl: String,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }
}