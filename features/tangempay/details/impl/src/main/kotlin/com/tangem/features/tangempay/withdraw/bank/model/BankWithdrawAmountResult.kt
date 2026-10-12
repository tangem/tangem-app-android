package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import java.math.BigDecimal

internal data class BankWithdrawAmountResult(
    val amount: BigDecimal,
    val source: BankWithdrawSource,
    val destination: OfframpDestination,
)