package com.tangem.tap.features.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.DialogFullScreen
import com.tangem.core.ui.decompose.ComposableContentComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Full-screen root-detected security warning. Presentational only — its visibility is controlled by the
 * startup gate (via a childSlot); "Continue" resolves [RootWarningContinuation]. Whether it should be shown
 * at all (and marking it as shown) is decided by the gate.
 */
@Suppress("UnusedPrivateProperty")
internal class RootDetectedWarningComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val rootWarningContinuation: RootWarningContinuation,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        DialogFullScreen(onDismissRequest = {}) {
            RootDetectedWarningContent(
                modifier = modifier,
                onContinueClick = remember(this) { ::onContinueClick },
            )
        }
    }

    private fun onContinueClick() {
        rootWarningContinuation.dismiss()
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Unit, RootDetectedWarningComponent> {
        override fun create(context: AppComponentContext, params: Unit): RootDetectedWarningComponent
    }
}