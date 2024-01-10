package com.tangem.managetokens.presentation.customtokens.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.AdditionalTextInputDialogParams
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.customtokens.state.EnterCustomDerivationState

@Composable
internal fun CustomDerivationDialog(state: EnterCustomDerivationState) {
    val confirmButton = DialogButton(
        title = stringResource(id = R.string.common_ok),
        enabled = state.confirmButtonEnabled,
        onClick = state.onConfirmButtonClick,
    )

    val dismissButton = DialogButton(
        title = stringResource(id = R.string.common_cancel),
        onClick = state.onDismiss,
    )

    val params = AdditionalTextInputDialogParams(
        placeholder = stringResource(id = R.string.custom_token_custom_derivation_placeholder),
        isError = state.derivationIncorrect,
        errorText = if (state.derivationIncorrect) {
            stringResource(R.string.custom_token_invalid_derivation_path)
        } else {
            null
        },
    )

    TextInputDialog(
        fieldValue = state.value,
        confirmButton = confirmButton,
        onDismissDialog = state.onDismiss,
        onValueChange = state.onValueChange,
        textFieldParams = params,
        title = stringResource(id = R.string.custom_token_custom_derivation_title),
        dismissButton = dismissButton,
    )
}