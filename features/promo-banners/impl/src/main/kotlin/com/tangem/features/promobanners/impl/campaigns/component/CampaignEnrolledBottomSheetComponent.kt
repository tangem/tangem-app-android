package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.onDismiss
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.components.bottomsheets.message.vector
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_success_24
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.campaignName

internal class CampaignEnrolledBottomSheetComponent(
    private val campaignType: CampaignType,
    private val onDismissRequest: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() = onDismissRequest()

    @Composable
    override fun BottomSheet() {
        MessageBottomSheet(
            state = messageBottomSheetUM {
                onDismiss(onDismissRequest)
                infoBlock {
                    vector(Icons.ic_success_24) {
                        type = MessageBottomSheetUM.Vector.Type.Accent
                        backgroundType = MessageBottomSheetUM.Vector.BackgroundType.SameAsTint
                    }

                    title = stringReference("You're successfully enrolled in ${campaignType.campaignName()}")
                    body = stringReference("Your cashback will be applied to eligible swaps automatically.")
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