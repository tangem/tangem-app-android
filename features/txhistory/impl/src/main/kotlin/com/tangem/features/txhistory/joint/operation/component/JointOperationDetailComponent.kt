package com.tangem.features.txhistory.joint.operation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.ui.TxHistoryDetailsContent
import kotlinx.coroutines.flow.StateFlow

/** A screen of the operation sheet; the state it draws is the root model's, there is nothing of its own to keep */
internal class JointOperationDetailComponent(
    appComponentContext: AppComponentContext,
    private val state: StateFlow<TxHistoryDetailsUM.SingleAsset?>,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by state.collectAsStateWithLifecycle()

        uiState?.let { TxHistoryDetailsContent(state = it, modifier = modifier) }
    }
}