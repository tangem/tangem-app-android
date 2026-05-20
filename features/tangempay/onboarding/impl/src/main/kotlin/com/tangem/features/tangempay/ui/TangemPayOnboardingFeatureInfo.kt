package com.tangem.features.tangempay.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TangemPayOnboardingBlock(
    @DrawableRes painterRes: Int,
    titleRef: TextReference,
    descriptionRef: TextReference,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(id = painterRes),
            contentDescription = null,
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
            tint = TangemTheme.colors.icon.accent,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = titleRef.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = descriptionRef.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}