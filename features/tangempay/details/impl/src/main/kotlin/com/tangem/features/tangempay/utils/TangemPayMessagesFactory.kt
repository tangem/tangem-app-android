package com.tangem.features.tangempay.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.core.ui.res.generated.icons.ic_snowflake_20
import com.tangem.core.ui.res.generated.icons.ic_sun_20
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType

internal object TangemPayMessagesFactory {

    fun createErrorMessage(errorType: TangemPayDetailsErrorType): BottomSheetMessage {
        return when (errorType) {
            TangemPayDetailsErrorType.Receive -> bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.img_attention_20) {
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Attention
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
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Attention
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
                        type = MessageBottomSheetUM.Icon.Type.Informative
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Informative
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

    fun createFreezeCardMessage(isRedesignEnabled: Boolean, onFreezeClicked: () -> Unit): BottomSheetMessage {
        return bottomSheetMessage {
            infoBlock {
                if (isRedesignEnabled) {
                    vector(Icons.ic_snowflake_20) {
                        type = MessageBottomSheetUM.Vector.Type.Informative
                        backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Informative
                    }
                } else {
                    icon(R.drawable.ic_snow_24) {
                        type = MessageBottomSheetUM.Icon.Type.Accent
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Accent
                    }
                }
                title = TextReference.Res(R.string.tangem_pay_freeze_card_alert_title)
                body = TextReference.Res(R.string.tangem_pay_freeze_card_alert_body)
            }
            if (isRedesignEnabled) {
                secondaryButton {
                    text = resourceReference(R.string.common_cancel)
                    onClick {
                        closeBs()
                    }
                }
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

    fun createUnfreezeCardMessage(isRedesignEnabled: Boolean, onUnfreezeClicked: () -> Unit): BottomSheetMessage {
        return bottomSheetMessage {
            infoBlock {
                if (isRedesignEnabled) {
                    vector(Icons.ic_sun_20) {
                        type = MessageBottomSheetUM.Vector.Type.Attention
                        backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Attention
                    }
                } else {
                    icon(R.drawable.ic_snow_24) {
                        type = MessageBottomSheetUM.Icon.Type.Accent
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Accent
                    }
                }
                title = TextReference.Res(R.string.tangem_pay_unfreeze_card_alert_title)
                body = TextReference.Res(R.string.tangem_pay_unfreeze_card_alert_body)
            }
            if (isRedesignEnabled) {
                secondaryButton {
                    text = resourceReference(R.string.common_cancel)
                    onClick {
                        closeBs()
                    }
                }
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

    fun createWithdrawWarning(onGotItClick: () -> Unit): BottomSheetMessage {
        return bottomSheetMessage {
            infoBlock {
                icon(R.drawable.img_attention_20) {
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Attention
                }
                title = TextReference.Res(R.string.tangempay_withdrawal_note_title)
                body = TextReference.Res(R.string.tangempay_withdrawal_note_description)
            }
            primaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick {
                    onGotItClick()
                    closeBs()
                }
            }
        }
    }

    fun createMaximumCardsIssued(maxCards: Int): BottomSheetMessage {
        return bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_error_28) {
                    type = MessageBottomSheetUM.Vector.Type.Attention
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Attention
                }
                title = resourceReference(R.string.tangempay_maximum_cards_issued_title)
                body = resourceReference(
                    id = R.string.tangempay_maximum_cards_issued_description,
                    formatArgs = wrappedList(maxCards),
                )
            }
            primaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }
    }

    fun createGenericError(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.common_something_went_wrong),
            message = resourceReference(R.string.common_try_again_later),
        )
    }

    fun createFutureFeature(onGotItClick: () -> Unit): BottomSheetMessage {
        return bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_credit_card_add_24) {
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Accent
                }
                title = resourceReference(R.string.tangempay_feature_will_be_available_soon)
                body = resourceReference(R.string.tangempay_feature_will_be_available_soon_description)
            }
            primaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick {
                    onGotItClick()
                    closeBs()
                }
            }
        }
    }
}