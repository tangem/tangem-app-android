package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.onDismiss
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.promobanners.impl.R

internal class NotActiveCampaignBottomSheetComponent(
    private val onDismissRequest: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() = onDismissRequest()

    @Composable
    override fun BottomSheet() {
        MessageBottomSheet(
            state = messageBottomSheetUM {
                onDismiss(onDismissRequest)
                infoBlock {
                    icon(R.drawable.ic_alert_circle_24) {
                        type = MessageBottomSheetUM.Icon.Type.Warning
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = stringReference("Campaign not active") // TODO localization
                    body = stringReference("This campaign no longer exists or has expired.") // TODO localization
                }
                primaryButton {
                    text = resourceReference(R.string.common_close)
                    onClick { closeBs() }
                }
            },
            onDismissRequest = onDismissRequest,
        )
    }
}