package com.tangem.features.send.mcp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.features.send.mcp.model.SendIntentButtonUM
import com.tangem.features.send.mcp.model.SendIntentUM

/**
 * Sheet of the intent flow. It stays hidden while the intent is being read, then shows the content of [state] and
 * the one action it carries. The last offer remains visible until the dismissal animation finishes.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=23363-122221)
 */
@Composable
internal fun SendIntentModal(state: SendIntentUM, onDismiss: () -> Unit) {
    val displayedOffer = rememberLastNonNull(state as? SendIntentUM.Offer)

    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = state !is SendIntentUM.Loading,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = { TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onDismiss) }) },
    ) {
        if (displayedOffer != null) {
            Column(modifier = Modifier.weight(weight = 1f, fill = false).verticalScroll(rememberScrollState())) {
                SendIntentOfferContent(state = displayedOffer)
            }
            SendIntentFooter(button = displayedOffer.primaryButton)
        }
    }
}

@Composable
private fun SendIntentFooter(button: SendIntentButtonUM) {
    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeEnabled = false,
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            text = button.text,
            onClick = button.onClick,
        )
    }
}