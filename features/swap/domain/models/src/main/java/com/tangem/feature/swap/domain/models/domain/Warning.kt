package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

sealed class Warning {

    data class ExistentialDepositWarning(val existentialDeposit: BigDecimal) : Warning()
}