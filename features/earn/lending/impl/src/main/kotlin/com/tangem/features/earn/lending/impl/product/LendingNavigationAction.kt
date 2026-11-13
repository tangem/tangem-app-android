package com.tangem.features.earn.lending.impl.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.session.EarnAction

// Temporary plain text in this file: replace with localized string resources.
internal sealed interface LendingNavigationAction : EarnAction {

    data object OpenPosition : LendingNavigationAction {
        override val id: String = "open_position"

        override val label: TextReference = stringReference("Open")
    }

    data object OpenPicker : LendingNavigationAction {
        override val id: String = "open_picker"

        override val label: TextReference = stringReference("Choose")
    }
}