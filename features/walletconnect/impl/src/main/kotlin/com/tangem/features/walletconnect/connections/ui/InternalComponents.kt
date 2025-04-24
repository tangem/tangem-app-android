package com.tangem.features.walletconnect.connections.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcAppInfoItem(
    iconUrl: String,
    title: String,
    subtitle: String,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(TangemTheme.dimens.spacing12),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size48)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
                .background(Color.Black),
            model = iconUrl,
            contentDescription = title,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = title,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h3,
                )
                if (isVerified) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.img_approvale2_20),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
            }
            Text(
                text = subtitle,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
            )
        }
    }
}