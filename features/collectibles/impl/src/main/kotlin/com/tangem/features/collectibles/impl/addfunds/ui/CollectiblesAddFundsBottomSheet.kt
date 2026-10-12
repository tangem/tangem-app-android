package com.tangem.features.collectibles.impl.addfunds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.res.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.addfunds.CollectiblesAddFundsUM

@Composable
internal fun CollectiblesAddFundsBottomSheet(state: CollectiblesAddFundsUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        scrollableContent = false,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.common_add_funds),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                onClose = state.onDismiss,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.items.fastForEach { item ->
                    key(item.title) {
                        TangemRow(
                            contentLead = TangemRowContentLead.Start,
                            startSlot = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TangemTheme.colors3.bg.status.infoSubtle),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TangemIcon(
                                        tangemIconUM = item.icon,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
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
                SpacerH(24.dp)
            }
        },
    )
}