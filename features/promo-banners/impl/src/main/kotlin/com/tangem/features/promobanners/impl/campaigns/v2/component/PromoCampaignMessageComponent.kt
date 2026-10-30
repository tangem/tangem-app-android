package com.tangem.features.promobanners.impl.campaigns.v2.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.message.modal.MessageModal
import com.tangem.core.ui.message.modal.MessageModalUM

/** A v2 campaign status sheet — not found, already enrolled — shown as a design-system status modal. */
internal class PromoCampaignMessageComponent(
    private val message: MessageModalUM,
    private val onDismiss: () -> Unit,
) : ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        MessageModal(state = message, onDismissRequest = onDismiss)
    }
}