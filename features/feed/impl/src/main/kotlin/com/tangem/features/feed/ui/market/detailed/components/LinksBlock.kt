package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.ChipShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.chip.Chip
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.badge.TangemBadgeIconPosition
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.ContainerWithDivider
import com.tangem.features.feed.ui.market.detailed.state.LinksUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun LinksBlock(state: LinksUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        LinksBlockV2(state, modifier)
    } else {
        LinksBlockV1(state, modifier)
    }
}

@Composable
private fun LinksBlockV1(state: LinksUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        contentHorizontalPadding = 0.dp,
        title = {
            Text(
                text = stringResourceSafe(id = R.string.markets_token_details_links),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        content = {
            Column {
                SubBlockV1(
                    title = stringResourceSafe(id = R.string.markets_token_details_official_links),
                    links = state.officialLinks,
                    onLinkClick = state.onLinkClick,
                )
                SubBlockV1(
                    title = stringResourceSafe(id = R.string.markets_token_details_social),
                    links = state.social,
                    onLinkClick = state.onLinkClick,
                )
                SubBlockV1(
                    title = stringResourceSafe(id = R.string.markets_token_details_repository),
                    links = state.repository,
                    onLinkClick = state.onLinkClick,
                )
                SubBlockV1(
                    title = stringResourceSafe(id = R.string.markets_token_details_blockchain_site),
                    links = state.blockchainSite,
                    onLinkClick = state.onLinkClick,
                    lastBlock = true,
                )
            }
        },
    )
}

@Composable
private fun LinksBlockV2(state: LinksUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SubBlockV2(
            title = stringResourceSafe(id = R.string.markets_token_details_official_links),
            links = state.officialLinks,
            onLinkClick = state.onLinkClick,
        )
        SubBlockV2(
            title = stringResourceSafe(id = R.string.markets_token_details_social),
            links = state.social,
            onLinkClick = state.onLinkClick,
        )
        SubBlockV2(
            title = stringResourceSafe(id = R.string.markets_token_details_repository),
            links = state.repository,
            onLinkClick = state.onLinkClick,
        )
        SubBlockV2(
            title = stringResourceSafe(id = R.string.markets_token_details_blockchain_site),
            links = state.blockchainSite,
            onLinkClick = state.onLinkClick,
            lastBlock = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubBlockV1(
    links: ImmutableList<LinksUM.Link>,
    onLinkClick: (LinksUM.Link) -> Unit,
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
            modifier = Modifier.padding(TangemTheme.dimens.spacing12),
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
                links.fastForEach { link ->
                    Chip(
                        text = stringReference(link.title),
                        iconResId = link.iconRes,
                        onClick = { onLinkClick(link) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubBlockV2(
    title: String,
    links: ImmutableList<LinksUM.Link>,
    onLinkClick: (LinksUM.Link) -> Unit,
    modifier: Modifier = Modifier,
    lastBlock: Boolean = false,
) {
    if (links.isEmpty()) return

    ContainerWithDivider(
        modifier = modifier,
        showDivider = !lastBlock,
    ) {
        Column(
            modifier = Modifier.padding(vertical = TangemTheme.dimens2.x2),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        ) {
            Text(
                modifier = Modifier.padding(start = 10.dp, top = TangemTheme.dimens2.x4),
                text = title,
                style = TangemTheme.typography2.bodySemibold16,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                modifier = Modifier.padding(vertical = TangemTheme.dimens2.x2, horizontal = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
            ) {
                links.fastForEach { link ->
                    TangemBadge(
                        text = stringReference(link.title),
                        onClick = { onLinkClick(link) },
                        iconPosition = TangemBadgeIconPosition.Start,
                        tangemIconUM = TangemIconUM.Icon(
                            iconRes = link.iconRes,
                            tintReference = { TangemTheme.colors2.markers.iconGray },
                        ),
                        size = TangemBadgeSize.X9,
                        shape = TangemBadgeShape.Rounded,
                    )
                }
            }
        }
    }
}

@Composable
fun LinksBlockPlaceholder(modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        LinksBlockPlaceholderV2(modifier)
    } else {
        LinksBlockPlaceholderV1(modifier)
    }
}

@Composable
private fun LinksBlockPlaceholderV1(modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        contentHorizontalPadding = 0.dp,
        title = {
            TextShimmer(
                modifier = Modifier.fillMaxWidth(),
                style = TangemTheme.typography.subtitle2,
            )
        },
        content = {
            Column {
                SubBlockPlaceholderV1()
                SubBlockPlaceholderV1()
                SubBlockPlaceholderV1(lastBlock = true)
            }
        },
    )
}

@Composable
private fun LinksBlockPlaceholderV2(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SubBlockPlaceholderV2()
        SubBlockPlaceholderV2()
        SubBlockPlaceholderV2(lastBlock = true)
    }
}

@Composable
private fun SubBlockPlaceholderV1(modifier: Modifier = Modifier, lastBlock: Boolean = false) {
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
                    ChipShimmer(
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubBlockPlaceholderV2(modifier: Modifier = Modifier, lastBlock: Boolean = false) {
    ContainerWithDivider(
        modifier = modifier,
        showDivider = !lastBlock,
    ) {
        Column(
            modifier = Modifier.padding(TangemTheme.dimens2.x2),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        ) {
            TextShimmer(
                modifier = Modifier.width(56.dp),
                style = TangemTheme.typography2.bodySemibold16,
                radius = TangemTheme.dimens2.x25,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
            ) {
                repeat(times = 3) {
                    ChipShimmer(
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreviewV1() {
    TangemThemePreview {
        LinksBlockV1(
            state = LinksUM(
                officialLinks = persistentListOf(
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                social = persistentListOf(
                    LinksUM.Link(
                        title = "Twitter",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Facebook",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                repository = persistentListOf(
                    LinksUM.Link(
                        title = "Github",
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
private fun ContentPreviewV2() {
    TangemThemePreviewRedesign {
        LinksBlockV2(
            state = LinksUM(
                officialLinks = persistentListOf(
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Website",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                social = persistentListOf(
                    LinksUM.Link(
                        title = "Twitter",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                    LinksUM.Link(
                        title = "Facebook",
                        iconRes = R.drawable.ic_plus_24,
                        url = "https://tangem.com",
                    ),
                ),
                repository = persistentListOf(
                    LinksUM.Link(
                        title = "Github",
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
private fun PlaceholderPreviewV1() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = { LinksBlockPlaceholderV1() },
            actualContent = { ContentPreviewV1() },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        Column {
            LinksBlockPlaceholderV2()
            ContentPreviewV2()
        }
    }
}