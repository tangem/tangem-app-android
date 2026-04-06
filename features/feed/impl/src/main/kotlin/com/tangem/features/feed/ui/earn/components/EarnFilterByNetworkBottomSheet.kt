package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.rows.RowContentContainer
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun EarnFilterByNetworkBottomSheet(config: TangemBottomSheetConfig) {
    if (LocalRedesignEnabled.current) {
        EarnFilterByNetworkBottomSheetV2(config = config)
    } else {
        EarnFilterByNetworkBottomSheetV1(config = config)
    }
}

@Composable
private fun EarnFilterByNetworkBottomSheetV1(config: TangemBottomSheetConfig) {
    TangemBottomSheet<EarnFilterByNetworkBottomSheetContentUM>(
        config = config,
        titleText = resourceReference(R.string.earn_filter_by),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { ContentV1(it) },
    )
}

@Composable
private fun EarnFilterByNetworkBottomSheetV2(config: TangemBottomSheetConfig) {
    EarnFilterBottomSheet<EarnFilterByNetworkBottomSheetContentUM>(
        config = config,
        content = { ContentV2(it) },
    )
}

@Composable
private fun ContentV1(content: EarnFilterByNetworkBottomSheetContentUM) {
    val allMyNetworks = remember(content) {
        content.networks.filterIsInstance<EarnFilterNetworkUM.AllNetworks>() +
            content.networks.filterIsInstance<EarnFilterNetworkUM.MyNetworks>()
    }
    val specificNetworks = remember(content) { content.networks.filterIsInstance<EarnFilterNetworkUM.Network>() }

    LazyColumn(
        contentPadding = PaddingValues(
            start = TangemTheme.dimens.spacing16,
            end = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        allMyNetworksList(
            allMyNetworks = allMyNetworks,
            onOptionClicked = content.onOptionClick,
        )

        if (specificNetworks.isNotEmpty()) {
            networksHeader()

            specificNetworksList(
                specificNetworks = specificNetworks,
                onOptionClicked = content.onOptionClick,
            )
        }
    }
}

@Composable
private fun ContentV2(content: EarnFilterByNetworkBottomSheetContentUM) {
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
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = when (item) {
                        is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                        is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                        is EarnFilterNetworkUM.Network -> TextReference.Str(item.text)
                    }.resolveReference(),
                    style = TangemTheme.typography2.bodySemibold16,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                TangemCheckbox(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .layoutId(layoutId = TangemRowLayoutId.TAIL),
                    isChecked = item.isSelected,
                    onCheckedChange = { onOptionClick(item) },
                )
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
                style = TangemTheme.typography2.bodyRegular14,
                color = TangemTheme.colors2.text.neutral.tertiary,
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
                        style = TangemTheme.typography2.bodySemibold16,
                        color = TangemTheme.colors2.text.neutral.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    TangemCheckbox(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .layoutId(layoutId = TangemRowLayoutId.TAIL),
                        isChecked = item.isSelected,
                        onCheckedChange = { onOptionClick(item) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.allMyNetworksList(
    allMyNetworks: List<EarnFilterNetworkUM>,
    onOptionClicked: (EarnFilterNetworkUM) -> Unit,
) {
    itemsIndexed(
        items = allMyNetworks,
        key = { _, item ->
            when (item) {
                is EarnFilterNetworkUM.AllNetworks -> "all_networks"
                is EarnFilterNetworkUM.MyNetworks -> "my_networks"
                is EarnFilterNetworkUM.Network -> item.id
            }
        },
    ) { index, item ->
        DividerContainer(
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = allMyNetworks.lastIndex,
                    addDefaultPadding = false,
                )
                .background(TangemTheme.colors.background.action)
                .clickable { onOptionClicked(item) },
            showDivider = index != allMyNetworks.lastIndex,
        ) {
            InputRowChecked(
                text = when (item) {
                    is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                    is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                    is EarnFilterNetworkUM.Network -> TextReference.Str(item.text)
                },
                checked = item.isSelected,
            )
        }
    }
}

private fun LazyListScope.networksHeader() {
    item(key = "networks_header") {
        SpacerH(TangemTheme.dimens.spacing16)

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    ),
                )
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 4.dp,
                ),
            text = stringResourceSafe(id = R.string.earn_filter_networks),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LazyListScope.specificNetworksList(
    specificNetworks: List<EarnFilterNetworkUM.Network>,
    onOptionClicked: (EarnFilterNetworkUM) -> Unit,
) {
    itemsIndexed(
        items = specificNetworks,
        key = { _, item -> item.id },
    ) { index, item ->
        DividerContainer(
            modifier = Modifier
                .height(52.dp)
                .roundedShapeItemDecoration(
                    currentIndex = index + 1,
                    lastIndex = specificNetworks.lastIndex + 1,
                    addDefaultPadding = false,
                )
                .background(TangemTheme.colors.background.action)
                .clickable { onOptionClicked(item) },
            showDivider = false,
        ) {
            RowContentContainer(
                modifier = Modifier
                    .heightIn(52.dp)
                    .padding(horizontal = 12.dp),
                icon = {
                    Image(
                        modifier = Modifier.size(22.dp),
                        imageVector = ImageVector.vectorResource(item.iconRes),
                        contentDescription = item.symbol,
                    )
                },
                text = {
                    RowText(
                        mainText = item.text,
                        secondText = item.symbol,
                        accentMainText = true,
                        accentSecondText = false,
                    )
                },
                action = {
                    if (item.isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24),
                            contentDescription = null,
                            tint = TangemTheme.colors.icon.accent,
                        )
                    }
                },
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV1() {
    TangemThemePreview(
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