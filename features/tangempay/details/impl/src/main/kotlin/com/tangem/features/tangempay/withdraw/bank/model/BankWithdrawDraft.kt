package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import java.math.BigDecimal

/** Everything the wizard has collected so far; each step fills its own part and the preview reads the whole draft. */
internal data class BankWithdrawDraft(
    val entry: BankWithdrawEntry,
    val recipient: BankAccount? = null,
    val amount: BigDecimal? = null,
    val source: BankWithdrawSource? = null,
    val destination: OfframpDestination? = null,
)