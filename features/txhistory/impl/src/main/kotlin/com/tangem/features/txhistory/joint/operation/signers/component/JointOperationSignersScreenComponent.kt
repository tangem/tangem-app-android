package com.tangem.features.txhistory.joint.operation.signers.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.txhistory.joint.operation.signers.ui.JointOperationSignersContent
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import kotlinx.coroutines.flow.StateFlow

/** Who has answered the operation — the same operation the first screen shows, read as a roster */
internal class JointOperationSignersScreenComponent(
    appComponentContext: AppComponentContext,
    private val state: StateFlow<JointOperationUM>,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by state.collectAsStateWithLifecycle()

        val content = uiState as? JointOperationUM.Content ?: return

        JointOperationSignersContent(state = content.signers, modifier = modifier)
    }
}