package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.CustomTokenDerivationInputComponent
import com.tangem.features.managetokens.entity.customtoken.CustomDerivationInputUM
import com.tangem.features.managetokens.ui.dialog.CustomDerivationInputDialog

internal class PreviewCustomTokenDerivationInputComponent(
    private val value: String = "",
    private val error: String? = null,
) : CustomTokenDerivationInputComponent {

    override fun dismiss() {
        /* no-op */
    }

    @Composable
    override fun Dialog() {
        val model = CustomDerivationInputUM(
            value = TextFieldValue(value),
            error = error?.let(::stringReference),
            updateValue = {},
            isConfirmEnabled = false,
            onConfirm = {},
        )

        CustomDerivationInputDialog(
            model = model,
            onDismiss = ::dismiss,
        )
    }
}