package com.tangem.domain.earn.model

import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias EarnTokensBatchingContext = BatchingContext<Int, EarnTokensListConfig, Nothing>

typealias EarnTokensBatchFlow = BatchFlow<Int, List<EarnTokenWithCurrency>, Nothing>