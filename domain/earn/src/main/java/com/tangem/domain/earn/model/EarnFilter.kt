package com.tangem.domain.earn.model

import kotlinx.serialization.Serializable

@Serializable
data class EarnFilter(
    val earnFilterNetwork: EarnFilterNetwork,
    val earnFilterType: EarnFilterType,
)