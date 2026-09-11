package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldError
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldKind
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.form.model.FormState
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.FieldUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayBankWithdrawFormUMFactory(
    private val fields: ImmutableList<BankWithdrawFormField>,
    private val step: TangemPayBankWithdrawInnerRoute,
    private val callbacks: Callbacks,
) {

    fun create(draft: BankWithdrawDraft, state: FormState): TangemPayBankWithdrawFormUM {
        val visibleFields = BankWithdrawFormFields.visible(fields = fields, values = state.values)
        return TangemPayBankWithdrawFormUM(
            header = BankWithdrawHeaderFactory.create(draft = draft, step = step),
            fields = visibleFields
                .mapIndexed { index, field ->
                    createField(field = field, state = state, isLast = index == visibleFields.lastIndex)
                }
                .toImmutableList(),
            isContinueEnabled = visibleFields.all { field -> !state.values[field].isNullOrBlank() },
            onBackClick = callbacks.onBackClick,
            onContinueClick = callbacks.onContinueClick,
        )
    }

    private fun createField(field: BankWithdrawFormField, state: FormState, isLast: Boolean): FieldUM {
        val error = state.errors[field]?.toText()
        val value = state.values[field].orEmpty()
        return when (val kind = field.kind) {
            is BankWithdrawFieldKind.TextField -> FieldUM.Text(
                field = field,
                value = value,
                error = error,
                keyboardOptions = KeyboardOptions(
                    capitalization = kind.capitalization,
                    keyboardType = kind.keyboardType,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                onValueChange = { newValue -> callbacks.onValueChange(field, newValue) },
                onFocusChange = { isFocused -> callbacks.onFocusChange(field, isFocused) },
            )
            BankWithdrawFieldKind.SelectorField -> FieldUM.Selector(
                field = field,
                value = value,
                error = error,
                onClick = { callbacks.onSelectorClick(field) },
            )
        }
    }

    private fun BankWithdrawFieldError.toText(): TextReference = when (this) {
        BankWithdrawFieldError.Required -> resourceReference(R.string.tangempay_order_data_field_required)
        BankWithdrawFieldError.InvalidFormat ->
            resourceReference(R.string.tangempay_withdraw_bank_field_error_invalid_format)
        BankWithdrawFieldError.InvalidRoutingNumber ->
            resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number)
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onValueChange: (field: BankWithdrawFormField, value: String) -> Unit,
        val onFocusChange: (field: BankWithdrawFormField, isFocused: Boolean) -> Unit,
        val onSelectorClick: (field: BankWithdrawFormField) -> Unit,
        val onContinueClick: () -> Unit,
    )
}