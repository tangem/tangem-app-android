package com.tangem.features.earn.api.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.persistentListOf

@Composable
fun EarnRowInfoModal(state: EarnRowInfoUM, onDismiss: () -> Unit) {
    TangemModal<EarnRowInfoUM>(
        config = TangemBottomSheetConfig(isShown = true, onDismissRequest = onDismiss, content = state),
        title = { content -> TangemModal.TopNavigation(title = content.title, onClose = onDismiss) },
    ) { content ->
        EarnRowInfoContent(state = content)
    }
}

@Composable
private fun EarnRowInfoContent(state: EarnRowInfoUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(TangemFooter.ModalWindowInsets.asPaddingValues())) {
        when (state) {
            is EarnRowInfoUM.Text -> Text(
                text = state.text.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.padding(16.dp),
            )
            is EarnRowInfoUM.Items -> state.items.forEachIndexed { index, item ->
                TangemRow(
                    divider = index < state.items.lastIndex,
                    titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
                    subtitleSlot = {
                        TangemRowText(
                            text = item.description,
                            role = TangemRowTextRole.Subtitle,
                            maxLines = Int.MAX_VALUE,
                        )
                    },
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnRowInfoItemsPreview() {
    TangemThemePreviewRedesign {
        EarnRowInfoContent(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            state = EarnRowInfoUM.Items(
                title = stringReference("Risk information"),
                items = persistentListOf(
                    EarnRowInfoUM.Item(
                        title = stringReference("Low risk"),
                        description = stringReference("High liquidity and shorter withdrawals."),
                    ),
                    EarnRowInfoUM.Item(
                        title = stringReference("Medium risk"),
                        description = stringReference("High liquidity, with withdrawals targeted within 48 hours."),
                    ),
                    EarnRowInfoUM.Item(
                        title = stringReference("High risk"),
                        description = stringReference("May use leverage and take market risk."),
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnRowInfoTextPreview() {
    TangemThemePreviewRedesign {
        EarnRowInfoContent(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            state = EarnRowInfoUM.Text(
                title = stringReference("Withdrawals"),
                text = stringReference("Withdrawals are usually instant, subject to the liquidity of the protocol."),
            ),
        )
    }
}
// endregion