package com.tangem.features.earn.impl.banner

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.product.api.rate.formatEarnRate

// Temporary plain text: replace with localized string resources.
internal object EarnBannerTexts {

    val title: TextReference = stringReference("Earn")

    val enable: TextReference = stringReference("Enable")

    val enabledTitle: TextReference = stringReference("Yield enabled")

    fun upTo(rate: EarnProductRate): TextReference =
        stringReference("Up to ${rate.fraction.formatEarnRate()}% ${rate.type.name}")

    fun enabledDescription(amount: String?, activeCount: Int): TextReference = stringReference(
        if (amount != null) "$amount · $activeCount active" else "$activeCount active",
    )
}