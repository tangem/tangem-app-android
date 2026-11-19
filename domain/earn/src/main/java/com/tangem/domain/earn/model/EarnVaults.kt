package com.tangem.domain.earn.model

import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType

fun List<EarnTokenWithCurrency>.withVaultsIf(isIncluded: Boolean): List<EarnTokenWithCurrency> {
    return if (isIncluded) this else filterNot { it.earnToken.type == EarnType.VAULT }
}