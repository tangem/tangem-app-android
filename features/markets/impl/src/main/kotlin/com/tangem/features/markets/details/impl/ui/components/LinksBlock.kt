package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.downsample.fastForEach
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.details.impl.ui.entity.LinksUM
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun LinksBlock(state: LinksUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.markets_token_details_links),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        content = {
            Column {
                SubBlock(
                    title = stringResource(id = R.string.markets_token_details_official_links),
                    links = state.officialLinks,
                    onLinkClick = state.onLinkClick,
                )
                SubBlock(
                    title = stringResource(id = R.string.markets_token_details_social),
                    links = state.social,
                    onLinkClick = state.onLinkClick,
                )
                SubBlock(
                    title = stringResource(id = R.string.markets_token_details_repository),
                    links = state.repository,
                    onLinkClick = state.onLinkClick,
                )
                SubBlock(
                    title = stringResource(id = R.string.markets_token_details_blockchain_site),
                    links = state.blockchainSite,
                    onLinkClick = state.onLinkClick,
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubBlock(
    links: ImmutableList<LinksUM.LinkUM>,
    onLinkClick: (LinksUM.LinkUM) -> Unit,
    modifier: Modifier = Modifier,
    lastBlock: Boolean = false,
    title: String = "Official links",
) {
    if (links.isEmpty()) return

    DividerContainer(
        modifier = modifier,
        showDivider = !lastBlock,
    ) {
        Column(
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            ) {
                links.fastForEach {
                    SecondarySmallButton(
                        config = SmallButtonConfig(
                            text = it.title,
                            onClick = { onLinkClick(it) },
                            icon = TangemButtonIconPosition.Start(iconResId = it.iconRes),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun LinksBlockPlaceholder(modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            TextShimmer(
                modifier = Modifier.fillMaxWidth(),
                style = TangemTheme.typography.subtitle2,
            )
        },
        content = {
            Column {
                SubBlockPlaceholder()
                SubBlockPlaceholder()
                SubBlockPlaceholder(lastBlock = true)
            }
        },
    )
}

@Composable
private fun SubBlockPlaceholder(modifier: Modifier = Modifier, lastBlock: Boolean = false) {
    DividerContainer(
        modifier = modifier,
        showDivider = !lastBlock,
    ) {
        Column(
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            TextShimmer(
                modifier = Modifier.width(78.dp),
                style = TangemTheme.typography.caption2,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            ) {
                repeat(times = 3) {
                    SmallButtonShimmer(
                        modifier = Modifier.weight(1f),
                        withIcon = true,
                    )
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        LinksBlock(
            state = LinksUM(
                officialLinks = persistentListOf(
                    LinksUM.LinkUM(
                        title = stringReference("Website"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.LinkUM(
                        title = stringReference("Website"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.LinkUM(
                        title = stringReference("Website"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                social = persistentListOf(
                    LinksUM.LinkUM(
                        title = stringReference("Twitter"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.LinkUM(
                        title = stringReference("Facebook"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                repository = persistentListOf(
                    LinksUM.LinkUM(
                        title = stringReference("Github"),
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                blockchainSite = persistentListOf(),
                onLinkClick = {},
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceholderPreview() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = { LinksBlockPlaceholder() },
            actualContent = { ContentPreview() },
        )
    }
}