package com.tangem.domain.swap.models

import java.math.BigDecimal

enum class PredefinedPercentAmount(val percent: BigDecimal) {
    PERCENT_25(BigDecimal("0.25")),
    PERCENT_50(BigDecimal("0.50")),
    PERCENT_75(BigDecimal("0.75")),
    MAX(BigDecimal.ONE),
}