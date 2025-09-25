package com.tangem.features.tangempay.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
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
    Row(
        modifier = modifier
            .padding(start = 32.dp, end = 32.dp),
    ) {
        Icon(
            painter = painterResource(id = painterRes),
            contentDescription = null,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp),
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
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