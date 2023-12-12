package com.tangem.feature.swap.domain.models.domain

enum class SwapApproveType {
    LIMITED, UNLIMITED
}

fun SwapApproveType.getNameForAnalytics(): String {
    return when (this) {
        SwapApproveType.LIMITED -> "Transaction"
        SwapApproveType.UNLIMITED -> "Unlimited"
    }
}