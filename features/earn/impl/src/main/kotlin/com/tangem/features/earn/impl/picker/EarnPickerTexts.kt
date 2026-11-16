package com.tangem.features.earn.impl.picker

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab

// Temporary plain text: replace with localized string resources.
internal object EarnPickerTexts {

    val title: TextReference = stringReference("Earn on your Ethereum")

    val subtitle: TextReference = stringReference("Choose type of product for gaining your balance")

    val benefitsSection: TextReference = stringReference("Benefits")

    val detailsSection: TextReference = stringReference("More details")

    val startEarning: TextReference = stringReference("Start earning")

    val addFunds: TextReference = stringReference("Add funds")

    val unavailable: TextReference = stringReference("Failed to load the product")

    val retry: TextReference = stringReference("Retry")

    fun tab(tab: EarnPickerTab): TextReference = stringReference(
        when (tab) {
            EarnPickerTab.Calculator -> "Calculator"
            EarnPickerTab.Benefits -> "Benefits"
            EarnPickerTab.Details -> "Details"
        },
    )
}