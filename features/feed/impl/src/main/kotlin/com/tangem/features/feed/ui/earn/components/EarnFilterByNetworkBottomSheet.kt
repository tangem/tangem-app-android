package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun EarnFilterByNetworkBottomSheet(config: TangemBottomSheetConfig) {
    EarnFilterBottomSheet<EarnFilterByNetworkBottomSheetContentUM>(
        config = config,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: EarnFilterByNetworkBottomSheetContentUM) {
    val allMyNetworks = remember(content) {
        (content.networks.filterIsInstance<EarnFilterNetworkUM.AllNetworks>() +
            content.networks.filterIsInstance<EarnFilterNetworkUM.MyNetworks>()).toImmutableList()
    }
    val specificNetworks = remember(content) {
        content
            .networks
            .filterIsInstance<EarnFilterNetworkUM.Network>()
            .toImmutableList()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworksTypesBlock(
            allMyNetworks = allMyNetworks,
            onOptionClick = content.onOptionClick,
        )

        SpecificNetworksBlock(
            specificNetworks = specificNetworks,
            onOptionClick = content.onOptionClick,
        )
    }
}

@Composable
private fun NetworksTypesBlock(
    allMyNetworks: ImmutableList<EarnFilterNetworkUM>,
    onOptionClick: (EarnFilterNetworkUM) -> Unit,
) {
    CardFilterBlock {
        allMyNetworks.fastForEachIndexed { index, item ->
            TangemRowContainer(
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = allMyNetworks.lastIndex,
                        addDefaultPadding = false,
                    )
                    .clickable { onOptionClick(item) },
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (index == 0) 18.dp else 12.dp,
                    bottom = if (index == allMyNetworks.lastIndex) {
                        18.dp
                    } else {
                        12.dp
                    },
                ),
            ) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = when (item) {
                        is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                        is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                        is EarnFilterNetworkUM.Network -> TextReference.Str(item.text)
                    }.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.isSelected) {
                    TangemCheckbox(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .layoutId(layoutId = TangemRowLayoutId.TAIL),
                        isChecked = true,
                        onCheckedChange = { onOptionClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecificNetworksBlock(
    specificNetworks: ImmutableList<EarnFilterNetworkUM.Network>,
    onOptionClick: (EarnFilterNetworkUM) -> Unit,
) {
    if (specificNetworks.isNotEmpty()) {
        CardFilterBlock {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                text = stringResourceSafe(id = R.string.earn_filter_networks),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            specificNetworks.fastForEachIndexed { index, item ->
                TangemRowContainer(
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = specificNetworks.lastIndex,
                            addDefaultPadding = false,
                        )
                        .clickable { onOptionClick(item) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Image(
                        modifier = Modifier
                            .size(40.dp)
                            .layoutId(TangemRowLayoutId.HEAD),
                        imageVector = ImageVector.vectorResource(item.iconRes),
                        contentDescription = item.symbol,
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .layoutId(layoutId = TangemRowLayoutId.START_TOP),
                        text = item.text,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (item.isSelected) {
                        TangemCheckbox(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .layoutId(layoutId = TangemRowLayoutId.TAIL),
                            isChecked = true,
                            onCheckedChange = { onOptionClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
            EarnFilterByNetworkBottomSheet(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = EarnFilterByNetworkBottomSheetContentUM(
                        networks = persistentListOf(
                            EarnFilterNetworkUM.AllNetworks(isSelected = true),
                            EarnFilterNetworkUM.MyNetworks(isSelected = false),
                            EarnFilterNetworkUM.Network(
                                id = "ethereum",
                                text = "Ethereum",
                                symbol = "ETH",
                                iconRes = R.drawable.img_btc_22,
                                isSelected = false,
                            ),
                            EarnFilterNetworkUM.Network(
                                id = "polygon",
                                text = "Polygon",
                                symbol = "MATIC",
                                iconRes = R.drawable.img_btc_22,
                                isSelected = false,
                            ),
                        ),
                        onOptionClick = {},
                    ),
                ),
            )
        }
    }
}