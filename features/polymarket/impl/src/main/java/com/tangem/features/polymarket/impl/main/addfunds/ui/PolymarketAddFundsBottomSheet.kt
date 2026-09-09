package com.tangem.features.polymarket.impl.main.addfunds.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsUM

@Composable
internal fun PolymarketAddFundsBottomSheet(state: PolymarketAddFundsUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.common_add_funds),
                type = TangemTopBarType.BottomSheet,
                endContent = null,
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
            ) {
                state.items.fastForEach { item ->
                    key(item.iconRes) {
                        TangemRow(
                            contentLead = TangemRowContentLead.Start,
                            startSlot = { TangemIcon(tangemIconUM = TangemIconUM.Icon(iconRes = item.iconRes)) },
                            titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
                            subtitleSlot = {
                                TangemRowText(
                                    text = item.description,
                                    role = TangemRowTextRole.Subtitle,
                                    maxLines = 2,
                                )
                            },
                            onClick = item.onClick,
                        )
                    }
                }
            }
        },
    )
}