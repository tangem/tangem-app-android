package com.tangem.features.earn.impl.picker

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import java.math.BigDecimal

// Temporary plain text: replace with localized string resources.
internal object EarnPickerTexts {

    val title: TextReference = stringReference("Earn on your Ethereum")

    val subtitle: TextReference = stringReference("Choose type of product for gaining your balance")

    val startEarning: TextReference = stringReference("Start earning")

    val addFunds: TextReference = stringReference("Add funds")

    fun minDepositHint(minDeposit: BigDecimal): TextReference = stringReference(
        "You don't have any funds on this token – add funds to continue. " +
            "Minimum amount – ${minDeposit.toPlainString()} ETH",
    )

    fun tab(tab: EarnPickerTab): TextReference = stringReference(
        when (tab) {
            EarnPickerTab.Calculator -> "Calculator"
            EarnPickerTab.Benefits -> "Benefits"
            EarnPickerTab.Details -> "Details"
        },
    )
}