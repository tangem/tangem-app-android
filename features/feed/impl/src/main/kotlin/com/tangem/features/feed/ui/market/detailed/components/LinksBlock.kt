package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.LinksUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun LinksBlock(state: LinksUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SubBlock(
            title = stringResourceSafe(id = R.string.markets_token_details_official_links),
            links = state.officialLinks,
            onLinkClick = state.onLinkClick,
        )
        SubBlock(
            title = stringResourceSafe(id = R.string.markets_token_details_social),
            links = state.social,
            onLinkClick = state.onLinkClick,
        )
        SubBlock(
            title = stringResourceSafe(id = R.string.markets_token_details_repository),
            links = state.repository,
            onLinkClick = state.onLinkClick,
        )
        SubBlock(
            title = stringResourceSafe(id = R.string.markets_token_details_blockchain_site),
            links = state.blockchainSite,
            onLinkClick = state.onLinkClick,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubBlock(
    title: String,
    links: ImmutableList<LinksUM.Link>,
    onLinkClick: (LinksUM.Link) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) return

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 16.dp),
            text = title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            links.fastForEach { link ->
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    onClick = { onLinkClick(link) },
                    text = stringReference(link.title),
                    iconStart = TangemIconUM.Icon(iconRes = link.iconRes),
                    size = TangemButton.Size.X9,
                )
            }
        }
    }
}

@Composable
fun LinksBlockPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SubBlockPlaceholder()
        SubBlockPlaceholder()
        SubBlockPlaceholder()
    }
}

@Composable
private fun SubBlockPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinksShimmerLine(
            style = TangemTheme.typography3.heading.small,
            width = 56.dp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(times = 3) {
                TangemShimmer(
                    radius = 999.dp,
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LinksShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreviewRedesign {
        LinksBlock(
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
private fun PlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        Column {
            LinksBlockPlaceholder()
            ContentPreview()
        }
    }
}