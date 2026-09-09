package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.ImeAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankWithdrawFormUM(
    val header: BankWithdrawHeaderUM,
    val fields: ImmutableList<FieldUM>,
    val isContinueEnabled: Boolean,
    val onBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
) {

    @Immutable
    data class FieldUM(
        val field: BankWithdrawFormField,
        val value: String,
        val error: TextReference?,
        val imeAction: ImeAction,
        val onValueChange: (String) -> Unit,
        val onFocusChange: (Boolean) -> Unit,
    )
}