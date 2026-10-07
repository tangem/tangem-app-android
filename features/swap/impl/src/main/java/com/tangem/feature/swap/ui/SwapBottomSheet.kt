package com.tangem.feature.swap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.reportMeasuredHeight
import com.tangem.core.ui.res.TangemTheme

/**
 * Renders one swap sheet inside a [TangemModal]. The card material never changes while the modal is shown: a glass
 * sheet is a separate modal stacked above the flat one.
 *
 * The modal sizes its card from the content's intrinsic height, so contents must not contain a `SubcomposeLayout`
 * (lazy lists, `BoxWithConstraints`, scaffolds, `EllipsisText`).
 */
@Composable
internal fun SwapBottomSheet(
    content: ComposableModularContentComponent,
    isMaterial: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        isMaterialEnabled = isMaterial,
        containerColor = if (isMaterial) TangemTheme.colors3.bg.opaque.primary else TangemTheme.colors3.bg.secondary,
        onBack = onBack,
        title = { content.Title() },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reportMeasuredHeight(),
            ) {
                content.Content(modifier = Modifier.fillMaxWidth())
                content.Footer()
            }
        },
    )
}