package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.rows.RowContentContainer
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun EarnFilterByNetworkBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<EarnFilterByNetworkBottomSheetContentUM>(
        config = config,
        titleText = resourceReference(R.string.earn_filter_by),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: EarnFilterByNetworkBottomSheetContentUM) {
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
                text = item.text,
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
                    CoinIcon(
                        modifier = Modifier.size(22.dp),
                        url = item.iconUrl,
                        alpha = 1f,
                        colorFilter = null,
                        fallbackResId = R.drawable.ic_custom_token_44,
                    )
                },
                text = {
                    RowText(
                        mainText = item.text.resolveReference(),
                        secondText = item.symbol.resolveReference(),
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
private fun Preview() {
    TangemThemePreview(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
            EarnFilterByNetworkBottomSheet(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = EarnFilterByNetworkBottomSheetContentUM(
                        selectedNetwork = EarnFilterNetworkUM.AllNetworks(
                            text = resourceReference(R.string.earn_filter_all_networks),
                            isSelected = true,
                        ),
                        networks = persistentListOf(
                            EarnFilterNetworkUM.AllNetworks(
                                text = resourceReference(R.string.earn_filter_all_networks),
                                isSelected = true,
                            ),
                            EarnFilterNetworkUM.MyNetworks(
                                text = stringReference("My network"),
                                isSelected = false,
                            ),
                            EarnFilterNetworkUM.Network(
                                id = "ethereum",
                                text = stringReference("Ethereum"),
                                symbol = stringReference("ETH"),
                                iconUrl = null,
                                isSelected = false,
                            ),
                            EarnFilterNetworkUM.Network(
                                id = "polygon",
                                text = stringReference("Polygon"),
                                symbol = stringReference("MATIC"),
                                iconUrl = null,
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