package com.tangem.features.earn.lending.impl.product

import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnProjection
import java.math.BigDecimal

internal fun LendingProvider.projectionOf(rate: BigDecimal): EarnProjection =
    DefaultEarnProjection(netRate = rate * (BigDecimal.ONE - performanceFee))