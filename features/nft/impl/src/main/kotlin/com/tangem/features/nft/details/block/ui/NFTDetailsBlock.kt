package com.tangem.features.nft.details.block.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.common.ui.NFTLogo
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTDetailsBlock(
    assetName: TextReference,
    collectionName: TextReference,
    assetImage: String?,
    networkIconRes: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "NFT Asset",
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.secondary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NFTLogo(
                assetImage,
                networkIconRes,
                background = TangemTheme.colors.background.action,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = assetName.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = collectionName.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NFTDetailsBlock_Preview() {
    TangemThemePreview {
        NFTDetailsBlock(
            assetName = stringReference("NFT Asset Name"),
            collectionName = stringReference("NFT Collection"),
            assetImage = null,
            networkIconRes = R.drawable.img_polygon_22,
        )
    }
}
// endregion