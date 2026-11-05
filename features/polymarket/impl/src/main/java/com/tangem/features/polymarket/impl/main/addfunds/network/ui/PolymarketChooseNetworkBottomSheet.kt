package com.tangem.features.polymarket.impl.main.addfunds.network.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.polymarket.impl.main.addfunds.network.PolymarketChooseNetworkUM

@Composable
internal fun PolymarketChooseNetworkBottomSheet(state: PolymarketChooseNetworkUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        title = {
            TangemModal.TopNavigation(title = resourceReference(R.string.common_choose_network))
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.networks.fastForEach { network ->
                    key(network.id) {
                        TangemRow(
                            contentLead = TangemRowContentLead.Start,
                            startSlot = {
                                TangemTokenIcon(state = network.icon, size = TangemTokenIcon.Size.X40)
                            },
                            titleSlot = { TangemRowText(text = network.name, role = TangemRowTextRole.Title) },
                            subtitleSlot = {
                                TangemRowText(text = network.symbol, role = TangemRowTextRole.Subtitle)
                            },
                            onClick = network.onClick,
                        )
                    }
                }
            }
        },
    )
}