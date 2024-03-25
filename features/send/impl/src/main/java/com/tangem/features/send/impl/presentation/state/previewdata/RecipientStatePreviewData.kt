package com.tangem.features.send.impl.presentation.state.previewdata

import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.persistentListOf

internal object RecipientStatePreviewData {

    val recipientState = SendStates.RecipientState(
        addressTextField = SendTextField.RecipientAddress(
            value = "0x23948239805671983476598176",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            placeholder = stringReference("Placeholder"),
            label = stringReference("Recipient"),
            isError = false,
            error = null,
        ),
        memoTextField = null,
        recent = persistentListOf(),
        wallets = persistentListOf(),
        network = "Ethereum",
        isValidating = false,
        isPrimaryButtonEnabled = true,
    )
}
