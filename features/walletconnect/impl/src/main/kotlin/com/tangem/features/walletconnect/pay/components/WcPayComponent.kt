package com.tangem.features.walletconnect.pay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.features.walletconnect.pay.model.WcPayModel
import com.tangem.features.walletconnect.pay.ui.WcPayBottomSheet
import kotlinx.serialization.Serializable

internal class WcPayComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcPayModel = getOrCreateModel(params)

    init {
        model.setDismissCallback(onDismiss)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        WcPayBottomSheet(
            state = state,
            onKycCompleted = model::onKycCompleted,
            onKycFailed = model::onKycFailed,
        )
    }

    @Serializable
    data class Params(val request: WcPairRequest)
}