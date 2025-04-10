package com.tangem.feature.qrscanning.presentation

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class QrScanningState(
    val message: TextReference?,
    val onQrScanned: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onGalleryClick: () -> Unit,
    val pasteAction: PasteAction = PasteAction.None,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,
)

@Immutable
internal sealed interface PasteAction {
    data object None : PasteAction
    class Perform(val action: () -> Unit) : PasteAction
}