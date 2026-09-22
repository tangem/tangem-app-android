package com.tangem.features.tangempay.withdraw.bank.form.model

import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft

/**
 * The user's local edits on this screen — the codes in [edits] and the names in [displayNameEdits] — plus the
 * errors shown under the fields and the [focusedField] the account-number suggestions are gated on; both edit maps
 * are cleared once Continue commits them, so a field with no local edit always reflects the draft's current value
 * instead of a stale snapshot from construction time.
 */
internal data class FormState(
    val edits: Map<BankWithdrawFormField, String> = emptyMap(),
    val displayNameEdits: Map<BankWithdrawFormField, String> = emptyMap(),
    val errors: Map<BankWithdrawFormField, BankWithdrawFieldError> = emptyMap(),
    val focusedField: BankWithdrawFormField? = null,
) {

    /** The value shown for [field]: the user's own typing on this screen, or the draft's current value. */
    fun value(field: BankWithdrawFormField, draft: BankWithdrawDraft): String =
        edits[field] ?: draft.newRecipient.fields[field].orEmpty()

    /** [fields] resolved the same way as [value] — what Continue or Back would commit. */
    fun values(fields: List<BankWithdrawFormField>, draft: BankWithdrawDraft): Map<BankWithdrawFormField, String> =
        fields.associateWith { field -> value(field, draft) }

    /** The name of what [value] holds the code of, or blank when the value has no name of its own. */
    fun displayName(field: BankWithdrawFormField, draft: BankWithdrawDraft): String =
        displayNameEdits[field] ?: draft.newRecipient.displayNames[field].orEmpty()

    /** [fields] resolved the same way as [displayName] — always the same key set as [values]. */
    fun displayNames(
        fields: List<BankWithdrawFormField>,
        draft: BankWithdrawDraft,
    ): Map<BankWithdrawFormField, String> = fields.associateWith { field -> displayName(field, draft) }
}