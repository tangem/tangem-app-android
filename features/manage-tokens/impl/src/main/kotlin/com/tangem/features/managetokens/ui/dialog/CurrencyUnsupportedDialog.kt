package com.tangem.features.managetokens.ui.dialog

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.managetokens.impl.R

@Composable
internal fun CurrencyUnsupportedDialog(title: TextReference, message: TextReference, onDismiss: () -> Unit) {
    BasicDialog(
        title = title.resolveReference(),
        message = message.resolveReference(),
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(R.string.common_ok),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
    )
}