package com.tangem.features.managetokens.ui.dialog

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.AdditionalTextInputDialogUM
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.CustomTokenDerivationInputComponent
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenDerivationInputComponent
import com.tangem.features.managetokens.entity.customtoken.CustomDerivationInputUM
import com.tangem.features.managetokens.impl.R

@Composable
internal fun CustomDerivationInputDialog(model: CustomDerivationInputUM, onDismiss: () -> Unit) {
    val value by rememberUpdatedState(newValue = model.value)

    TextInputDialog(
        title = stringResourceSafe(id = R.string.custom_token_custom_derivation_title),
        fieldValue = value,
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_ok),
            enabled = model.isConfirmEnabled,
            onClick = model.onConfirm,
        ),
        dismissButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_cancel),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
        onValueChange = model.updateValue,
        textFieldParams = AdditionalTextInputDialogUM(
            label = model.error?.resolveReference() ?: stringResourceSafe(id = R.string.custom_token_derivation_path),
            isError = model.error != null,
        ),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CustomDerivationInputDialog(
    @PreviewParameter(ComponentPreviewProvider::class) component: CustomTokenDerivationInputComponent,
) {
    TangemThemePreview {
        component.Dialog()
    }
}

private class ComponentPreviewProvider : PreviewParameterProvider<CustomTokenDerivationInputComponent> {
    override val values: Sequence<CustomTokenDerivationInputComponent>
        get() = sequenceOf(
            PreviewCustomTokenDerivationInputComponent(),
            PreviewCustomTokenDerivationInputComponent(
                value = "m/44'/60'/0'/0/0",
            ),
            PreviewCustomTokenDerivationInputComponent(
                value = "m/44'/60'/0'/0/0",
                error = "Invalid derivation path",
            ),
        )
}
// endregion Preview