package com.tangem.feature.qrscanning.presentation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class QrScanningState(
    val topBarConfig: TopBarConfig,
    val message: TextReference?,
    val onQrScanned: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onGalleryClick: () -> Unit,
    val pasteAction: PasteAction = PasteAction.None,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,
)

internal data class TopBarConfig(val title: TextReference?, @DrawableRes val startIcon: Int)

@Immutable
internal sealed interface PasteAction {
    data object None : PasteAction
    class Perform(val action: () -> Unit) : PasteAction
}