package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_warning_28
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId

internal fun kycRequiredBottomSheet(onContactSupportClick: () -> Unit, onContinueVerificationClick: () -> Unit) =
    bottomSheetMessage {
        infoBlock {
            vector(Icons.ic_warning_28) {
                type = MessageBottomSheetUM.Vector.Type.Warning
                backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Warning
            }
            title = resourceReference(R.string.tangempay_action_required_title)
            body = resourceReference(R.string.tangempay_action_required_description)
        }
        secondaryButton {
            text = resourceReference(R.string.common_contact_support)
            onClick {
                onContactSupportClick()
                closeBs()
            }
        }
        primaryButton {
            text = resourceReference(R.string.tangempay_action_required_primary_btn)
            onClick {
                onContinueVerificationClick()
                closeBs()
            }
        }
    }

internal fun kycRequiredFeedbackEmailType(userWalletId: UserWalletId, customerId: String?): FeedbackEmailType {
    val walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId)
    return if (customerId != null) {
        FeedbackEmailType.Visa.DirectUserRequest(walletMetaInfo = walletMetaInfo, customerId = customerId)
    } else {
        FeedbackEmailType.DirectUserRequest(walletMetaInfo = walletMetaInfo)
    }
}