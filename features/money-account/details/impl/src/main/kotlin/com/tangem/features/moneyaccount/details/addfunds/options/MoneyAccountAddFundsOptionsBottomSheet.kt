package com.tangem.features.moneyaccount.details.addfunds.options

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_government_20
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.features.moneyaccount.details.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun MoneyAccountAddFundsOptionsBottomSheet(state: MoneyAccountAddFundsOptionsUM, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.common_add_funds),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                onClose = onDismiss,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.items.fastForEach { item ->
                    key(item.title) {
                        OptionRow(item = item)
                    }
                }
                SpacerH(24.dp)
            }
        },
    )
}

@Composable
private fun OptionRow(item: MoneyAccountAddFundsOptionUM) {
    TangemRow(
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
                TangemIcon(tangemIconUM = item.icon, modifier = Modifier.size(20.dp))
            }
        },
        titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = item.description, role = TangemRowTextRole.Subtitle, maxLines = 2) },
        endSlot = loaderSlot.takeIf { item.isLoading },
        onClick = item.onClick,
    )
}

private val loaderSlot: @Composable BoxScope.() -> Unit = { TangemLoader() }

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountAddFundsOptionsBottomSheetPreview() {
    TangemThemePreviewRedesign {
        MoneyAccountAddFundsOptionsBottomSheet(
            state = MoneyAccountAddFundsOptionsUM(
                items = persistentListOf(
                    previewOption(
                        icon = Icons.ic_grid_20,
                        title = "From another wallet or exchange",
                        description = "Send USDC Polygon to your account's address",
                    ),
                    previewOption(
                        icon = Icons.ic_government_20,
                        title = "From a bank account",
                        description = "Receive USD from a bank account via ACH/Wire",
                        isLoading = true,
                    ),
                ),
            ),
            onDismiss = {},
        )
    }
}

private fun previewOption(icon: ImageVector, title: String, description: String, isLoading: Boolean = false) =
    MoneyAccountAddFundsOptionUM(
        icon = TangemIconUM.Icon(imageVector = icon, tintReference = { TangemTheme.colors3.icon.brand }),
        title = stringReference(title),
        description = stringReference(description),
        onClick = {},
        isLoading = isLoading,
    )