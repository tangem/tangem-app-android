package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.extensions.TextReference

data class TokenBalanceSegmentedButtonConfig(
    val title: TextReference,
    val type: BalanceType,
)

enum class BalanceType {
    ALL,
    AVAILABLE,
}