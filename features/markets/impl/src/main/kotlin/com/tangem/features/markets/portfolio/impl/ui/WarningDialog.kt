package com.tangem.features.markets.portfolio.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R

@Composable
internal fun WarningDialog(
    message: TextReference,
    onDismiss: () -> Unit,
    title: TextReference = resourceReference(R.string.common_warning),
) {
    BasicDialog(
        title = title.resolveReference(),
        message = message.resolveReference(),
        confirmButton = DialogButtonUM(
            title = stringResource(R.string.common_ok),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
    )
}