package com.tangem.features.earn.impl.sheet

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.product.EarnRisk

// Temporary plain text: replace with localized string resources.
internal object EarnYieldSheetTexts {

    val title: TextReference = stringReference("ETH yield")

    val inactiveTitle: TextReference = stringReference("Not active yet")

    val startEarning: TextReference = stringReference("Start earning")

    fun risk(risk: EarnRisk): TextReference = stringReference(
        when (risk) {
            EarnRisk.Low -> "Low risk"
            EarnRisk.Medium -> "Medium risk"
            EarnRisk.High -> "High risk"
        },
    )
}