package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
fun RequestFromItem(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing12, start = TangemTheme.dimens.spacing12),
        text = stringResourceSafe(R.string.wc_request_from),
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.subtitle2,
    )
}