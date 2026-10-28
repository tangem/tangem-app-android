package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun AboutSignaturesBS(threshold: Int, total: Int, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onDismiss) })
        },
        content = {
            AboutSignaturesContent(threshold = threshold, total = total, onGotItClick = onDismiss)
        },
    )
}

@Composable
private fun ColumnScope.AboutSignaturesContent(threshold: Int, total: Int, onGotItClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(weight = 1f, fill = false)
            .verticalScroll(rememberScrollState()),
    ) {
        TangemModalStatus(
            variant = TangemModalStatus.Variant.Info,
            title = resourceReference(CoreUiR.string.joint_account_invite_members_about_signatures_title),
            description = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_about_signatures_description,
                formatArgs = wrappedList(threshold, total),
            ),
        )
    }
    TangemFooter(
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeEnabled = false,
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_got_it),
            onClick = onGotItClick,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AboutSignatures() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            AboutSignaturesContent(threshold = 2, total = 5, onGotItClick = {})
        }
    }
}
// endregion