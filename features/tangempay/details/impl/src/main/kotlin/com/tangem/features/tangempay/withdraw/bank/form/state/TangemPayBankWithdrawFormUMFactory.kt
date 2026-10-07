package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.model.BankAccountSuggestions
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldError
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldKind
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.FormState
import com.tangem.features.tangempay.withdraw.bank.form.model.kindIn
import com.tangem.features.tangempay.withdraw.bank.form.model.placeholderIn
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.FieldUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.SuggestionUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.toShortName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayBankWithdrawFormUMFactory(
    private val fields: ImmutableList<BankWithdrawFormField>,
    private val step: TangemPayBankWithdrawInnerRoute,
    private val callbacks: Callbacks,
) {

    fun create(draft: BankWithdrawDraft, state: FormState, accounts: List<BankAccount>): TangemPayBankWithdrawFormUM {
        val values = state.values(fields, draft)
        val displayNames = state.displayNames(fields, draft)
        val suggestions = createSuggestions(values = values, state = state, accounts = accounts)
        return TangemPayBankWithdrawFormUM(
            header = BankWithdrawHeaderFactory.create(
                draft = draft,
                step = step,
                onCloseClick = callbacks.onCloseClick,
            ),
            fields = fields
                .mapIndexed { index, field ->
                    createField(
                        field = field,
                        values = values,
                        displayNames = displayNames,
                        error = state.errors[field]?.toText(),
                        isLast = index == fields.lastIndex,
                    )
                }
                .toImmutableList(),
            suggestions = suggestions,
            isContinueEnabled = suggestions.isNotEmpty() || fields.all { field -> !values[field].isNullOrBlank() },
            onBackClick = callbacks.onBackClick,
            onContinueClick = callbacks.onContinueClick,
        )
    }

    private fun createSuggestions(
        values: Map<BankWithdrawFormField, String>,
        state: FormState,
        accounts: List<BankAccount>,
    ): ImmutableList<SuggestionUM> {
        val field = BankWithdrawFormField.AccountNumber
        if (field !in fields || state.focusedField != field) return persistentListOf()
        val prefix = values[field].orEmpty().trim()
        return BankAccountSuggestions.match(accounts = accounts, prefix = prefix)
            .map { account ->
                SuggestionUM(
                    id = account.id,
                    matchedPrefix = prefix,
                    accountNumberSuffix = account.details.accountNumber.removePrefix(prefix),
                    name = account.details.beneficiary.toShortName(),
                    onClick = { callbacks.onSuggestionClick(account) },
                )
            }
            .toImmutableList()
    }

    private fun createField(
        field: BankWithdrawFormField,
        values: Map<BankWithdrawFormField, String>,
        displayNames: Map<BankWithdrawFormField, String>,
        error: TextReference?,
        isLast: Boolean,
    ): FieldUM = when (val kind = field.kindIn(values)) {
        is BankWithdrawFieldKind.TextField -> FieldUM.Text(
            field = field,
            value = values[field].orEmpty(),
            placeholder = field.placeholderIn(values),
            error = error,
            keyboardOptions = KeyboardOptions(
                capitalization = kind.capitalization,
                keyboardType = kind.keyboardType,
                imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
            ),
            contentType = field.contentType,
            onValueChange = { newValue -> callbacks.onValueChange(field, newValue) },
            onFocusChange = { isFocused -> callbacks.onFocusChange(field, isFocused) },
        )
        BankWithdrawFieldKind.SelectorField -> FieldUM.Selector(
            field = field,
            // A picked country or state is shown by name; the code is the fallback so the field never goes blank.
            value = displayNames[field].orEmpty().ifBlank { values[field].orEmpty() },
            placeholder = field.placeholderIn(values),
            error = error,
            onClick = { callbacks.onSelectorClick(field) },
        )
    }

    private fun BankWithdrawFieldError.toText(): TextReference = when (this) {
        BankWithdrawFieldError.Required -> resourceReference(R.string.tangempay_order_data_field_required)
        BankWithdrawFieldError.InvalidFormat ->
            resourceReference(R.string.tangempay_withdraw_bank_field_error_invalid_format)
        BankWithdrawFieldError.InvalidRoutingNumber ->
            resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number)
        BankWithdrawFieldError.NonLatin -> resourceReference(R.string.tangempay_order_data_field_latin_only)
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onValueChange: (field: BankWithdrawFormField, value: String) -> Unit,
        val onFocusChange: (field: BankWithdrawFormField, isFocused: Boolean) -> Unit,
        val onSelectorClick: (field: BankWithdrawFormField) -> Unit,
        val onSuggestionClick: (account: BankAccount) -> Unit,
        val onContinueClick: () -> Unit,
    )
}