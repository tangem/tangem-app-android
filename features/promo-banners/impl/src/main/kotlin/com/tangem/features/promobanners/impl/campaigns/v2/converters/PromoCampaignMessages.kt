package com.tangem.features.promobanners.impl.campaigns.v2.converters

import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.components.bottomsheets.message.vector
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_28
import com.tangem.core.ui.res.generated.icons.ic_warning_28
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.features.promobanners.impl.R

/** The v2 campaign status sheets, rendered by `MessageBottomSheetV3`. */
internal object PromoCampaignMessages {

    fun notFound(): MessageBottomSheetUM = messageBottomSheetUM {
        infoBlock {
            vector(Icons.ic_warning_28) { type = MessageBottomSheetUM.Vector.Type.Attention }
            title = resourceReference(R.string.promo_campaign_not_active_title)
            body = resourceReference(R.string.promo_campaign_not_active_subtitle)
        }
        closeButton()
    }

    /** The campaign's `nameKey` is already localized by the backend, so it goes into the title as-is. */
    fun alreadyEnrolled(campaign: PromoCampaign): MessageBottomSheetUM = messageBottomSheetUM {
        infoBlock {
            vector(Icons.ic_info_28) { type = MessageBottomSheetUM.Vector.Type.Informative }
            title = resourceReference(
                R.string.promo_campaign_already_activated_title,
                wrappedList(campaign.content.nameKey),
            )
        }
        closeButton()
    }

    private fun MessageBottomSheetUM.closeButton() {
        primaryButton {
            text = resourceReference(R.string.common_close)
            onClick { closeBs() }
        }
    }
}