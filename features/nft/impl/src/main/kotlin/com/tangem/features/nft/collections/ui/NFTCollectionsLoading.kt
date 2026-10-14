package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.impl.R

private const val SHIMMER_ITEMS_COUNT = 5

@Composable
internal fun NFTCollectionsLoading(state: NFTCollectionsUM.Loading, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Column {
            SearchBar(
                state = state.search,
                colors = TangemSearchBarDefaults.secondaryTextFieldColors,
                enabled = false,
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TangemTheme.colors.background.primary,
                    contentColor = TangemTheme.colors.text.primary1,
                    disabledContainerColor = TangemTheme.colors.background.primary,
                    disabledContentColor = TangemTheme.colors.text.primary1,
                ),
            ) {
                Column {
                    repeat(SHIMMER_ITEMS_COUNT) {
                        CollectionPlaceholder()
                    }
                }
            }
        }
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            text = stringResourceSafe(R.string.nft_collections_receive),
            onClick = state.onReceiveClick,
        )
    }
}

@Composable
private fun CollectionPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(
                horizontal = 12.dp,
                vertical = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TangemTheme.colors.field.primary),
                )
            }
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 12.dp,
                    )
                    .weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                TextShimmer(
                    modifier = Modifier.width(110.dp),
                    style = TangemTheme.typography.caption2,
                    textSizeHeight = true,
                )
                TextShimmer(
                    modifier = Modifier
                        .width(80.dp)
                        .padding(top = 4.dp),
                    style = TangemTheme.typography.caption2,
                    textSizeHeight = true,
                )
            }
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionsLoading() {
    TangemThemePreview {
        NFTCollectionsLoading(
            state = NFTCollectionsUM.Loading(
                search = SearchBarUM(
                    placeholderText = resourceReference(R.string.common_search),
                    query = "",
                    isActive = false,
                    onQueryChange = { },
                    onActiveChange = { },
                ),
                onReceiveClick = { },
            ),
        )
    }
}