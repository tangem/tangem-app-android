package com.tangem.features.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.details.impl.R

internal data class SelectEmailFeedbackTypeBS(
    val onOptionClick: (Option) -> Unit,
) : TangemBottomSheetConfigContent {

    enum class Option(val text: TextReference) {
        General(resourceReference(R.string.common_contact_tangem_support)),
        Visa(resourceReference(R.string.common_contact_visa_support)),
    }
}