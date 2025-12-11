package com.tangem.features.tangempay.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessageV2
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType

internal object TangemPayMessagesFactory {

    fun createErrorMessage(errorType: TangemPayDetailsErrorType): BottomSheetMessageV2 {
        return when (errorType) {
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
            TangemPayDetailsErrorType.Withdraw -> bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.img_attention_20) {
                        backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
                    }
                    title = TextReference.Res(R.string.tangempay_card_details_withdraw_error_title)
                    body = TextReference.Res(R.string.tangempay_card_details_receive_error_description)
                }
                primaryButton {
                    text = resourceReference(R.string.common_got_it)
                    onClick { closeBs() }
                }
            }
            TangemPayDetailsErrorType.WithdrawInProgress -> bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.ic_clock_24) {
                        type = MessageBottomSheetUMV2.Icon.Type.Informative
                        backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Informative
                    }
                    title = TextReference.Res(R.string.tangempay_card_details_withdraw_in_progress_title)
                    body = TextReference.Res(R.string.tangempay_card_details_withdraw_in_progress_description)
                }
                secondaryButton {
                    text = resourceReference(R.string.common_got_it)
                    onClick { closeBs() }
                }
            }
        }
    }

    fun createFreezeCardMessage(onFreezeClicked: () -> Unit): BottomSheetMessageV2 {
        return bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_snow_24) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Accent
                }
                title = TextReference.Res(R.string.tangem_pay_freeze_card_alert_title)
                body = TextReference.Res(R.string.tangem_pay_freeze_card_alert_body)
            }
            primaryButton {
                text = resourceReference(R.string.tangem_pay_freeze_card_freeze)
                onClick {
                    onFreezeClicked()
                    closeBs()
                }
            }
        }
    }

    fun createUnfreezeCardMessage(onUnfreezeClicked: () -> Unit): BottomSheetMessageV2 {
        return bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_snow_24) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Accent
                }
                title = TextReference.Res(R.string.tangem_pay_unfreeze_card_alert_title)
                body = TextReference.Res(R.string.tangem_pay_unfreeze_card_alert_body)
            }
            primaryButton {
                text = resourceReference(R.string.tangempay_card_details_unfreeze_card)
                onClick {
                    onUnfreezeClicked()
                    closeBs()
                }
            }
        }
    }
}