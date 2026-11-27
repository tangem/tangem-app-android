package com.tangem.features.earn.impl.picker.model

import java.math.BigDecimal

internal sealed interface EarnPickerDepositCapacity {

    data object Unknown : EarnPickerDepositCapacity

    data object Loading : EarnPickerDepositCapacity

    data class Known(val availableMax: BigDecimal) : EarnPickerDepositCapacity
}