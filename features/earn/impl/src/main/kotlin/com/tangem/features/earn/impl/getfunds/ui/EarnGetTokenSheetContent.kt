package com.tangem.features.earn.impl.getfunds.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_down_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.features.earn.impl.getfunds.EarnGetTokenSheetUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun EarnGetTokenSheetContent(state: EarnGetTokenSheetUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        title = {
            TangemTopNavigation(
                title = state.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                onClose = state.onDismiss,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.rows.fastForEach { row ->
                    key(row.title) { GetTokenRow(row = row) }
                }
                SpacerH(16.dp)
            }
        },
    )
}

@Composable
private fun GetTokenRow(row: EarnGetTokenSheetUM.Row) {
    TangemRow(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (row.isEnabled) 1f else DISABLED_ROW_ALPHA),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.status.infoSubtle),
                contentAlignment = Alignment.Center,
            ) {
                TangemIcon(
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = row.icon,
                        tintReference = { TangemTheme.colors3.icon.brand },
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        titleSlot = { TangemRowText(text = row.title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = row.subtitle, role = TangemRowTextRole.Subtitle, maxLines = 2) },
        endSlot = {
            if (row.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TangemTheme.colors3.icon.secondary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.ic_chevron_right_20,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TangemTheme.colors3.icon.secondary,
                )
            }
        },
        onClick = row.onClick.takeUnless { row.isLoading },
    )
}

private const val DISABLED_ROW_ALPHA = 0.4f

@Preview(name = "Light", widthDp = 360, heightDp = 480)
@Preview(name = "Dark", widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnGetTokenSheetContentPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .background(TangemTheme.colors3.bg.primary),
        ) {
            EarnGetTokenSheetContent(
                state = EarnGetTokenSheetUM(
                    title = stringReference("Get ETH"),
                    rows = persistentListOf(
                        EarnGetTokenSheetUM.Row(
                            icon = Icons.ic_arrow_down_20,
                            title = stringReference("Deposit ETH"),
                            subtitle = stringReference("Receive to your wallet from anywhere"),
                            isLoading = true,
                            isEnabled = true,
                            onClick = {},
                        ),
                        EarnGetTokenSheetUM.Row(
                            icon = Icons.ic_arrow_up_down_20,
                            title = stringReference("Swap to ETH"),
                            subtitle = stringReference("Convert another token you already hold"),
                            isLoading = false,
                            isEnabled = false,
                            onClick = {},
                        ),
                        EarnGetTokenSheetUM.Row(
                            icon = Icons.ic_card_20,
                            title = stringReference("Buy ETH"),
                            subtitle = stringReference("Card or bank, via trusted partner"),
                            isLoading = false,
                            isEnabled = true,
                            onClick = {},
                        ),
                    ),
                    onDismiss = {},
                ),
            )
        }
    }
}