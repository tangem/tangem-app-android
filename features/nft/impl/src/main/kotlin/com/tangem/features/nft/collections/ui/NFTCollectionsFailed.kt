package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTCollectionsFailed(state: NFTCollectionsUM.Failed, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TangemTheme.dimens.spacing16),
    ) {
        UnableToLoadData(
            modifier = Modifier
                .align(Alignment.Center),
            onRetryClick = state.onRetryClick,
        )
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            text = stringResourceSafe(R.string.nft_collections_receive),
            onClick = state.onReceiveClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionsFailed() {
    TangemThemePreview {
        NFTCollectionsFailed(
            state = NFTCollectionsUM.Failed(
                onRetryClick = { },
                onReceiveClick = { },
            ),
        )
    }
}