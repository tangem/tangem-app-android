package com.tangem.features.tangempay.withdraw.bank.form.state

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Immutable
import androidx.compose.ui.autofill.ContentType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankWithdrawFormUM(
    val header: BankWithdrawHeaderUM,
    val fields: ImmutableList<FieldUM>,
    val suggestions: ImmutableList<SuggestionUM>,
    val isContinueEnabled: Boolean,
    val onBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
) {

    /** A saved recipient whose account number starts with what the user has typed into the account-number field. */
    @Immutable
    data class SuggestionUM(
        val id: String,
        val matchedPrefix: String,
        val accountNumberSuffix: String,
        val name: String,
        val onClick: () -> Unit,
    )

    @Immutable
    sealed interface FieldUM {

        val field: BankWithdrawFormField
        val value: String
        val placeholder: TextReference
        val error: TextReference?

        data class Text(
            override val field: BankWithdrawFormField,
            override val value: String,
            override val placeholder: TextReference,
            override val error: TextReference?,
            val keyboardOptions: KeyboardOptions,
            val contentType: ContentType?,
            val onValueChange: (String) -> Unit,
            val onFocusChange: (Boolean) -> Unit,
        ) : FieldUM

        data class Selector(
            override val field: BankWithdrawFormField,
            override val value: String,
            override val placeholder: TextReference,
            override val error: TextReference?,
            val onClick: () -> Unit,
        ) : FieldUM
    }
}