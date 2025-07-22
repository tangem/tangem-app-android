package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model

import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.swap.v2.impl.R

internal object SwapChooseTokenFactory {

    fun getErrorMessage(tokenName: String, onDismiss: () -> Unit): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_attention_20) {
                    type = MessageBottomSheetUMV2.Icon.Type.Attention
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.express_swap_not_supported_title, wrappedList(tokenName))
                body = resourceReference(R.string.express_swap_not_supported_text)
            }
            secondaryButton {
                text = resourceReference(R.string.warning_button_ok)
                onClick { onDismiss() }
            }
        }
    }
}