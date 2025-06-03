package com.tangem.features.onboarding.v2.visa.impl.common

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage

val unexpectedErrorAlertBS
    get() = bottomSheetMessage {
        infoBlock {
            icon(R.drawable.img_knight_shield_32) {
                type = MessageBottomSheetUMV2.Icon.Type.Attention
                backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
            }
            title = resourceReference(R.string.unexpected_error_title)
            body = resourceReference(R.string.unexpected_error_description)
        }
        secondaryButton {
            text = resourceReference(R.string.common_got_it)
            onClick { closeBs() }
        }
    }