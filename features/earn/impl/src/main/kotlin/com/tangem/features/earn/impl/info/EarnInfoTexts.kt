package com.tangem.features.earn.impl.info

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab

// Temporary plain text: replace with localized string resources.
internal object EarnInfoTexts {

    val myFundsSection: TextReference = stringReference("My Funds")

    val benefitsSection: TextReference = stringReference("Benefits")

    val detailsSection: TextReference = stringReference("More details")

    val rewardsDescription: TextReference = stringReference("This total includes rewards from all incentivised tokens")

    val howItWorks: TextReference = stringReference("How it works")

    fun rewardsTitle(amount: String): TextReference = stringReference("You have $amount in rewards")

    fun staleNotice(asOf: String): TextReference = stringReference("Last updated $asOf")

    fun earnedChip(amount: String): TextReference = stringReference("Earned $amount")
}

internal fun EarnInfoTab.label(): TextReference = stringReference(
    when (this) {
        EarnInfoTab.MyFunds -> "My Funds"
        EarnInfoTab.Benefits -> "Benefits"
        EarnInfoTab.Details -> "Details"
    },
)