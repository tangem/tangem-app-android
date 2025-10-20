package com.tangem.features.tangempay.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessageV2
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType

internal object TangemPayErrorMessageFactory {

    fun createErrorMessage(type: TangemPayDetailsErrorType): BottomSheetMessageV2 {
        return when (type) {
            TangemPayDetailsErrorType.Receive -> bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.img_attention_20) {
                        backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
                    }
                    title = TextReference.Res(R.string.tangempay_card_details_receive_error_title)
                    body = TextReference.Res(R.string.tangempay_card_details_receive_error_description)
                }
                primaryButton {
                    text = resourceReference(R.string.common_got_it)
                    onClick { closeBs() }
                }
            }
        }
    }
}