package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.HoldToConfirm
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContent
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modal.tangemModalStatusUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingRevokeConfirmationUM

@Composable
internal fun PolymarketRevokeConfirmationSheet(state: FastTradingRevokeConfirmationUM) {
    val content = remember(state.description) {
        tangemModalStatusUM {
            variant = TangemModalStatus.Variant.Warning
            title = resourceReference(R.string.prediction_session_key_revoke_confirm_title)
            description = state.description
        }
    }
    val actionScope = remember(state.onDismiss) { TangemModalActionScope(state.onDismiss) }

    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            content = TangemBottomSheetConfigContent.Empty,
            onDismissRequest = state.onDismiss,
        ),
        onBack = state.onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                endButton = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = false)) {
            TangemModalContent(
                state = content,
                actionScope = actionScope,
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            )
            RevokeFooter(state = state)
        }
    }
}

@Composable
private fun RevokeFooter(state: FastTradingRevokeConfirmationUM) {
    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeVariant = TangemFade.Variant.Hard,
        fadeEnabled = false,
    ) {
        TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
            val revokeText = resourceReference(R.string.prediction_session_key_revoke)
            if (state.isHoldToConfirm) {
                TangemButton.HoldToConfirm(
                    variant = TangemButton.HoldVariant.Primary,
                    size = TangemButton.Size.X12,
                    text = revokeText,
                    onConfirm = state.onConfirm,
                )
            } else {
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = revokeText,
                    onClick = state.onConfirm,
                )
            }
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_cancel),
                onClick = state.onDismiss,
            )
        }
    }
}

@Preview(name = "Hold", widthDp = 360, heightDp = 640)
@Preview(name = "Hold dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketRevokeConfirmationSheetHoldPreview() {
    TangemThemePreviewRedesign {
        PolymarketRevokeConfirmationSheet(previewRevokeConfirmation(isHoldToConfirm = true))
    }
}

@Preview(name = "Tap", widthDp = 360, heightDp = 640)
@Composable
private fun PolymarketRevokeConfirmationSheetTapPreview() {
    TangemThemePreviewRedesign {
        PolymarketRevokeConfirmationSheet(previewRevokeConfirmation(isHoldToConfirm = false))
    }
}

private fun previewRevokeConfirmation(isHoldToConfirm: Boolean) = FastTradingRevokeConfirmationUM(
    description = stringReference("Fast trading will stop working on Pixel 9."),
    isHoldToConfirm = isHoldToConfirm,
    onConfirm = {},
    onDismiss = {},
)