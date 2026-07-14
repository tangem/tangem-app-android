package com.tangem.features.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.details.impl.R

internal data class SelectContactSupportTypeBS(
    val onOptionClick: (Option) -> Unit,
) : TangemBottomSheetConfigContent {

    enum class Option(val text: TextReference) {
        Mail(resourceReference(R.string.support_selector_view_email_button)),
        Chat(resourceReference(R.string.support_selector_view_chat_button)),
    }
}