package com.tangem.common.ui.amountScreen.models

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.Amount

/**
 * Model for amount field
 *
 * @param value entered value
 * @param onValueChange on value change
 * @param keyboardOptions keyboard options
 * @param keyboardActions keyboard actions
 * @param cryptoAmount value as amount
 * @param fiatAmount value in fiat as amount
 * @param isFiatValue indicates if app currency or crypto currency is selected
 * @param fiatValue value in fiat
 * @param isFiatUnavailable indicates if fiat rates are unavailable
 * @param isValuePasted indicated if value was pasted
 * @param onValuePastedTriggerDismiss  on value pasted action
 * @param isError indicates is value invalid
 * @param error error text
 */
data class AmountFieldModel(
    val value: String,
    val onValueChange: (String) -> Unit,
    val keyboardOptions: KeyboardOptions,
    val keyboardActions: KeyboardActions,
    val cryptoAmount: Amount,
    val fiatAmount: Amount,
    val isFiatValue: Boolean,
    val fiatValue: String,
    val isFiatUnavailable: Boolean,
    val isValuePasted: Boolean,
    val onValuePastedTriggerDismiss: () -> Unit,
    val isError: Boolean,
    val isWarning: Boolean,
    val error: TextReference,
)