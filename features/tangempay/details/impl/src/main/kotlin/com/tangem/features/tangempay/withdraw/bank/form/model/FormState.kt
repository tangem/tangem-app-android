package com.tangem.features.tangempay.withdraw.bank.form.model

import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft

/**
 * The user's local edits on this screen plus the errors shown under the fields; [edits] is cleared once
 * Continue commits it, so a field with no local edit always reflects the draft's current value instead of
 * a stale snapshot from construction time.
 */
internal data class FormState(
    val edits: Map<BankWithdrawFormField, String> = emptyMap(),
    val errors: Map<BankWithdrawFormField, BankWithdrawFieldError> = emptyMap(),
) {

    /** The value shown for [field]: the user's own typing on this screen, or the draft's current value. */
    fun value(field: BankWithdrawFormField, draft: BankWithdrawDraft): String =
        edits[field] ?: draft.newRecipient.fields[field].orEmpty()

    /** [fields] resolved the same way as [value] — what Continue or Back would commit. */
    fun values(fields: List<BankWithdrawFormField>, draft: BankWithdrawDraft): Map<BankWithdrawFormField, String> =
        fields.associateWith { field -> value(field, draft) }
}