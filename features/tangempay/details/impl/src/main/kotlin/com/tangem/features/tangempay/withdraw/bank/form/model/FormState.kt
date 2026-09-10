package com.tangem.features.tangempay.withdraw.bank.form.model

/** Raw field input of one form step plus the errors currently shown under the fields. */
internal data class FormState(
    val values: Map<BankWithdrawFormField, String>,
    val errors: Map<BankWithdrawFormField, BankWithdrawFieldError> = emptyMap(),
)