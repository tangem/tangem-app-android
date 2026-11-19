package com.tangem.features.tangempay.card.closure

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.message.modal.messageModalUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.tangempay.details.impl.R

@Suppress("LongParameterList")
internal fun closeCardConfirmModalUM(
    cardName: String?,
    lastDigits: String,
    canFreeze: Boolean,
    isClosingInProgress: Boolean,
    onCloseCardClick: () -> Unit,
    onFreezeInsteadClick: () -> Unit,
    onKeepCardClick: () -> Unit,
): MessageModalUM = messageModalUM {
    status {
        variant = TangemModalStatus.Variant.Error
        title = resourceReference(
            id = R.string.tangempay_close_card_sheet_title,
            formatArgs = wrappedList(listOfNotNull(cardName, "••$lastDigits").joinToString(separator = " ")),
        )
        description = resourceReference(
            if (canFreeze) {
                R.string.tangempay_close_card_sheet_description_with_freeze_hint
            } else {
                R.string.tangempay_close_card_sheet_description
            },
        )
        glyph(Icons.ic_error_28)
    }
    secondaryButton {
        text = resourceReference(R.string.tangempay_close_card_sheet_confirm)
        isLoading = isClosingInProgress
        onClick { onCloseCardClick() }
    }
    primaryButton {
        if (canFreeze) {
            text = resourceReference(R.string.tangempay_close_card_sheet_freeze_instead)
            onClick { onFreezeInsteadClick() }
        } else {
            text = resourceReference(R.string.tangempay_close_card_sheet_keep)
            onClick {
                onKeepCardClick()
                dismiss()
            }
        }
    }
}

internal fun closeCardErrorModalUM(onContactSupportClick: () -> Unit, onOkClick: () -> Unit): MessageModalUM =
    messageModalUM {
        status {
            variant = TangemModalStatus.Variant.Error
            title = resourceReference(R.string.tangempay_card_close_error_title)
            description = resourceReference(R.string.tangempay_card_close_error_message)
            glyph(Icons.ic_error_28)
        }
        secondaryButton {
            text = resourceReference(R.string.common_contact_support)
            onClick {
                onContactSupportClick()
                dismiss()
            }
        }
        primaryButton {
            text = resourceReference(R.string.common_ok)
            onClick {
                onOkClick()
                dismiss()
            }
        }
    }