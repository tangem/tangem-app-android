package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcTransactionRequestButtons(
    activeButtonText: TextReference,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = stringResourceSafe(R.string.common_cancel),
            onClick = onDismiss,
        )
        PrimaryButtonIconEnd(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = activeButtonText.resolveReference(),
            onClick = onSign,
            iconResId = R.drawable.ic_tangem_24,
            showProgress = isLoading,
        )
    }
}