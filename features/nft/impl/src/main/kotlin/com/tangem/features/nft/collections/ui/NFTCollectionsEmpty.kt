package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTCollectionsEmpty(state: NFTCollectionsUM.Empty, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(TangemTheme.dimens.spacing16),
    ) {
        Column(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing16)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_nft_placeholder_76),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing24),
                text = stringResource(R.string.nft_collections_empty_title),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                text = stringResource(R.string.nft_collections_empty_description),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing48)
                    .wrapContentWidth(),
                text = stringResourceSafe(R.string.nft_collections_receive),
                onClick = state.onReceiveClick,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionsEmpty() {
    TangemThemePreview {
        NFTCollectionsEmpty(
            state = NFTCollectionsUM.Empty(
                onReceiveClick = { },
            ),
        )
    }
}