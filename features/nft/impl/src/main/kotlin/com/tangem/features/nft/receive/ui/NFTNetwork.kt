package com.tangem.features.nft.receive.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.impl.R
import com.tangem.features.nft.receive.entity.NFTNetworkUM

@Composable
internal fun NFTNetwork(state: NFTNetworkUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { state.onItemClick() }
            .padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .size(TangemTheme.dimens.size24)
                .clip(CircleShape),
            painter = painterResource(state.iconRes),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            text = state.name,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTNetwork(@PreviewParameter(NFTNetworkProvider::class) state: NFTNetworkUM) {
    TangemThemePreview {
        NFTNetwork(
            state = state,
        )
    }
}

private class NFTNetworkProvider : CollectionPreviewParameterProvider<NFTNetworkUM>(
    collection = listOf(
        NFTNetworkUM(
            id = "item1",
            name = "Nethers #0853",
            iconRes = R.drawable.img_eth_22,
            onItemClick = { },
        ),
    ),
)