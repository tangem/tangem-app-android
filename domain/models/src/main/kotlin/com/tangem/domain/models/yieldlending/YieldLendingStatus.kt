package com.tangem.domain.models.yieldlending

import kotlinx.serialization.Serializable

@Serializable
data class YieldLendingStatus(
    val isActive: Boolean,
    val isInitialized: Boolean,
    val isAllowedToSpend: Boolean,
)