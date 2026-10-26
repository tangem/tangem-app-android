package com.tangem.feature.swap.mcp.ui

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
import com.tangem.feature.swap.mcp.model.ExchangeIntentButtonUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentUM

/**
 * Sheet of the intent flow: the offer in [state] and the one action it carries. It is shown for as long as it stays
 * in composition, so the caller opens and closes it by adding it to and removing it from composition.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=23363-122221)
 */
@Composable
internal fun ExchangeIntentModal(state: ExchangeIntentUM, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = { TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onDismiss) }) },
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = false).verticalScroll(rememberScrollState())) {
            ExchangeIntentOfferContent(state = state)
        }
        ExchangeIntentFooter(button = state.primaryButton)
    }
}

@Composable
private fun ExchangeIntentFooter(button: ExchangeIntentButtonUM) {
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