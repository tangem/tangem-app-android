package com.tangem.features.walletconnect.transaction.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
fun WcSmallTitleItem(@StringRes textRex: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp),
        text = stringResourceSafe(textRex),
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.subtitle2,
    )
}