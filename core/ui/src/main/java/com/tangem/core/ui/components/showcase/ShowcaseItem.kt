package com.tangem.core.ui.components.showcase

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ShowcaseItem(@DrawableRes iconRes: Int, title: TextReference, subtitle: TextReference) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TangemTheme.dimens.spacing20),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}