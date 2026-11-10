package com.tangem.features.earn.impl.incentive

import arrow.core.Either
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod

internal data class EarnIncentiveSheetRequest(
    val rate: EarnProductRate,
    val periods: List<EarnPeriod>,
    val history: suspend (EarnPeriod) -> Either<Throwable, EarnApyHistory>,
)