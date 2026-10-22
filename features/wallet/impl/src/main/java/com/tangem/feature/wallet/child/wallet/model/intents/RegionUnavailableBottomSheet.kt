package com.tangem.feature.wallet.child.wallet.model.intents

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_28

internal fun regionUnavailableBottomSheet(
    onContactSupportClick: () -> Unit,
    secondaryAction: EventMessageAction?,
    onHideAccountClick: () -> Unit,
) = bottomSheetMessage {
    val contactSupportLink = styledResourceReference(
        id = R.string.common_contact_support,
        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.primary) },
        onClick = onContactSupportClick,
    )
    infoBlock {
        vector(Icons.ic_heart_broken_28) {
            type = MessageBottomSheetUM.Vector.Type.Warning
            backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Warning
        }
        title = resourceReference(R.string.money_account_main_status_dialog_country_not_supported_title)
        body = resourceReference(
            id = R.string.money_account_main_status_dialog_country_not_supported_description_template,
            formatArgs = wrappedList(contactSupportLink),
        )
    }
    if (secondaryAction != null) {
        secondaryButton {
            text = secondaryAction.title
            onClick {
                secondaryAction.onClick()
                closeBs()
            }
        }
    }
    primaryButton {
        text = resourceReference(R.string.money_account_main_status_dialog_hide_account)
        onClick {
            onHideAccountClick()
            closeBs()
        }
    }
}