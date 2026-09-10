package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccountType

/** Answers collected for a recipient the user is adding; each wizard step fills its own part. */
internal data class NewRecipientDraft(
    /** Only the personal flow asks this; a business recipient never sees the Type of transfer step. */
    val isThirdParty: Boolean = false,
    val accountType: BankAccountType = BankAccountType.CHECKING,
)