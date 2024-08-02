package com.tangem.core.ui.components.showcase

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ShowcaseItem(@DrawableRes iconRes: Int, text: TextReference) {
    Row {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TangemTheme.dimens.spacing20),
        )
    }
}