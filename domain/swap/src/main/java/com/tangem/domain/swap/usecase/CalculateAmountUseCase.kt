package com.tangem.domain.swap.usecase

import com.tangem.domain.swap.models.PredefinedPercentAmount
import java.math.BigDecimal
import java.math.RoundingMode

class CalculateAmountUseCase {

    operator fun invoke(balance: BigDecimal, decimals: Int, percent: PredefinedPercentAmount): BigDecimal {
        return balance
            .multiply(percent.percent)
            .setScale(decimals, RoundingMode.DOWN)
    }
}