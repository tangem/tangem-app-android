package com.tangem.features.promobanners.impl.campaigns.v2.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV3
import com.tangem.core.ui.decompose.ComposableContentComponent

/** A v2 campaign status sheet — not found, already enrolled — shown as a design-system status modal. */
internal class PromoCampaignMessageComponent(
    private val message: MessageBottomSheetUM,
    private val onDismiss: () -> Unit,
) : ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        MessageBottomSheetV3(state = message, onDismissRequest = onDismiss)
    }
}