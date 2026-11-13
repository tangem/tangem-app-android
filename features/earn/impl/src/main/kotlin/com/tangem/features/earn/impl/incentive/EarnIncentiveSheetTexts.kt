package com.tangem.features.earn.impl.incentive

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference

// Temporary plain text: replace with localized string resources.
internal object EarnIncentiveSheetTexts {

    const val SERVICE_FEE_PERCENT = 15

    val title: TextReference = stringReference("Incentivised yield")

    val poweredByTitle: TextReference = stringReference("Powered by Merkl")

    val poweredByDescription: TextReference = stringReference(
        "A campaign sponsor funds extra reward tokens, distributed through Merkl.\n" +
            "Incentives may end, and reward token prices can change.\n" +
            "The displayed APY includes incentive rewards after Tangem's $SERVICE_FEE_PERCENT% service fee.",
    )
}