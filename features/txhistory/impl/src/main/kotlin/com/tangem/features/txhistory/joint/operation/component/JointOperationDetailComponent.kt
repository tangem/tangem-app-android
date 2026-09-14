package com.tangem.features.txhistory.joint.operation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.features.txhistory.ui.TxHistoryDetailsContent
import kotlinx.coroutines.flow.StateFlow

/** A screen of the operation sheet; the state it draws is the root model's, there is nothing of its own to keep */
internal class JointOperationDetailComponent(
    appComponentContext: AppComponentContext,
    private val state: StateFlow<JointOperationUM>,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by state.collectAsStateWithLifecycle()

        val content = uiState as? JointOperationUM.Content ?: return

        TxHistoryDetailsContent(state = content.operation, modifier = modifier)
    }
}