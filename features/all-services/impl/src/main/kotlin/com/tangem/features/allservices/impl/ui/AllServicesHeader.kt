package com.tangem.features.allservices.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.features.allservices.impl.R

@Composable
internal fun AllServicesHeader(
    onAddWalletClick: () -> Unit,
    onBuyTangemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResourceSafe(R.string.all_services_header_title),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.all_services_header_description),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderAction(
                title = resourceReference(R.string.user_wallet_add_wallet),
                imageVector = Icons.ic_sign_plus_24,
                onClick = onAddWalletClick,
                modifier = Modifier.weight(1f),
            )
            HeaderAction(
                title = resourceReference(R.string.introduction_button_buy_tangem),
                imageVector = Icons.ic_logo_tangem_24,
                onClick = onBuyTangemClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeaderAction(
    title: TextReference,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.secondary),
        includeInnerPaddings = false,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    .size(32.dp)
                    .background(color = TangemTheme.colors3.bg.accent.blueSubtle, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = imageVector, contentDescription = null, tint = TangemTheme.colors3.icon.accent.blue)
            }
        },
        titleSlot = {
            Text(
                text = title.resolveReference(),
                modifier = Modifier.padding(end = 16.dp),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.subheading.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AllServicesHeader_Preview() {
    TangemThemePreviewRedesign {
        AllServicesHeader(onAddWalletClick = {}, onBuyTangemClick = {})
    }
}