package com.tangem.features.earn.lending.impl.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.product.api.session.EarnAction

/** Routes the click of a row to a screen. It is never drawn as a button, so it has no label. */
internal sealed interface LendingNavigationAction : EarnAction {

    override val label: TextReference
        get() = TextReference.EMPTY

    data object OpenPosition : LendingNavigationAction {
        override val id: String = "open_position"
    }

    data object OpenPicker : LendingNavigationAction {
        override val id: String = "open_picker"
    }
}