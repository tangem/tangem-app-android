package com.tangem.features.earn.impl.widgets.calculator

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.product.EarnHorizon

// Temporary plain text: replace with localized string resources.
internal object EarnCalculatorTexts {

    val initialBalance: TextReference = stringReference("Initial balance")

    val monthlyTopUp: TextReference = stringReference("Monthly top-up")

    val earnedIn: TextReference = stringReference("earned in")

    fun horizon(horizon: EarnHorizon): TextReference = stringReference(
        when (horizon) {
            EarnHorizon.Months6 -> "6 months"
            EarnHorizon.Year1 -> "1 year"
            EarnHorizon.Year3 -> "3 years"
            EarnHorizon.Year5 -> "5 years"
            EarnHorizon.Year10 -> "10 years"
        },
    )

    fun decrease(label: TextReference): TextReference = combinedReference(stringReference("Decrease "), label)

    fun increase(label: TextReference): TextReference = combinedReference(stringReference("Increase "), label)

    fun axisMonths(months: Int): TextReference = stringReference("${months}M")

    fun axisYears(years: Int): TextReference = stringReference("${years}Y")
}