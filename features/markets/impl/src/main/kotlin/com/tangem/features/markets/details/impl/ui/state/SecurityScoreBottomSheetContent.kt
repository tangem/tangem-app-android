package com.tangem.features.markets.details.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class SecurityScoreBottomSheetContent(
    val title: TextReference,
    val description: TextReference,
    val providers: List<SecurityScoreProviderUM>,
    val onProviderLinkClick: (String) -> Unit,
) : TangemBottomSheetConfigContent {

    data class SecurityScoreProviderUM(
        val name: String,
        val lastAuditDate: String?,
        val score: Float,
        val fullProviderUrl: String?,
        val hostProviderUrl: String?,
        val iconUrl: String?,
    )
}
