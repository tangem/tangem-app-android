package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Immutable
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
    sealed interface FieldUM {

        val field: BankWithdrawFormField
        val value: String
        val error: TextReference?

        data class Text(
            override val field: BankWithdrawFormField,
            override val value: String,
            override val error: TextReference?,
            val keyboardOptions: KeyboardOptions,
            val onValueChange: (String) -> Unit,
            val onFocusChange: (Boolean) -> Unit,
        ) : FieldUM

        data class Selector(
            override val field: BankWithdrawFormField,
            override val value: String,
            override val error: TextReference?,
            val onClick: () -> Unit,
        ) : FieldUM
    }
}