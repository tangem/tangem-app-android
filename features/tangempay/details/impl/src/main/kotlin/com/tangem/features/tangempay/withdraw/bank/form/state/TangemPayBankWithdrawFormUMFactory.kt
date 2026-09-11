package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.ui.text.input.ImeAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldError
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
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

    fun create(draft: BankWithdrawDraft, state: FormState): TangemPayBankWithdrawFormUM = TangemPayBankWithdrawFormUM(
        header = BankWithdrawHeaderFactory.create(draft = draft, step = step),
        fields = fields
            .mapIndexed { index, field ->
                FieldUM(
                    field = field,
                    value = state.values[field].orEmpty(),
                    error = state.errors[field]?.toText(),
                    imeAction = if (index == fields.lastIndex) ImeAction.Done else ImeAction.Next,
                    onValueChange = { value -> callbacks.onValueChange(field, value) },
                    onFocusChange = { isFocused -> callbacks.onFocusChange(field, isFocused) },
                )
            }
            .toImmutableList(),
        isContinueEnabled = fields.all { field -> !state.values[field].isNullOrBlank() },
        onBackClick = callbacks.onBackClick,
        onContinueClick = callbacks.onContinueClick,
    )

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
        val onContinueClick: () -> Unit,
    )
}