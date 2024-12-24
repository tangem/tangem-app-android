package com.tangem.features.managetokens.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R

@Composable
internal fun AddCustomTokenDescription(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(fraction = 0.7f),
        text = stringResourceSafe(id = R.string.custom_token_subtitle),
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
}