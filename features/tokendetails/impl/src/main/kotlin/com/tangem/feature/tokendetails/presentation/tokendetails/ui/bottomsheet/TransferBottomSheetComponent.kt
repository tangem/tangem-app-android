package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import kotlinx.coroutines.flow.StateFlow
import com.tangem.core.ui.R as CoreR

internal class TransferBottomSheetComponent(
    private val stateFlow: StateFlow<TransferUM>,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by stateFlow.collectAsStateWithLifecycle()

        val config = remember(state) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = state,
            )
        }

        TangemModalBottomSheet<TransferUM>(
            config = config,
            containerColor = TangemTheme.colors2.surface.level2,
            title = {
                TangemTopBar(
                    title = resourceReference(CoreR.string.common_transfer),
                    type = TangemTopBarType.BottomSheet,
                    endContent = {
                        TangemButton(
                            iconStart = TangemIconUM.Icon(iconRes = CoreR.drawable.ic_close_24),
                            onClick = ::dismiss,
                            size = TangemButton.Size.X11,
                            variant = TangemButton.Variant.Material,
                        )
                    },
                )
            },
            content = { contentState ->
                TransferBottomSheetContent(
                    state = contentState,
                    onCloseClick = ::dismiss,
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
                )
            },
        )
    }
}