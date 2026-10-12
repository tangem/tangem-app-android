package com.tangem.features.tangempay.withdraw.bank.form.model

/** What a form step commits: codes for the payload, plus the name shown for each of the step's fields. */
internal data class BankWithdrawFormResult(
    val values: Map<BankWithdrawFormField, String>,
    val displayNames: Map<BankWithdrawFormField, String>,
)