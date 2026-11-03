package com.tangem.features.earn.api

import com.tangem.domain.models.earn.EarnProductId

sealed interface EarnEntryStart {

    /** Only for the For You card when Lending or Vault won: opens Info of the active product, else the picker. */
    data object Auto : EarnEntryStart

    data class Picker(val preselected: EarnProductId? = null) : EarnEntryStart

    data class Info(val productId: EarnProductId) : EarnEntryStart
}