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
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
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
    LinksBlockV2(state, modifier)
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
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubBlockV2(
    title: String,
    links: ImmutableList<LinksUM.Link>,
    onLinkClick: (LinksUM.Link) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) return

    Column(
        modifier = modifier.padding(vertical = TangemTheme.dimens2.x2),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = TangemTheme.dimens2.x4),
            text = title,
            style = TangemTheme.typography2.headingSemibold20,
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
                SecondaryTangemButton(
                    onClick = { onLinkClick(link) },
                    text = stringReference(link.title),
                    iconPosition = TangemButtonIconPosition.Start,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = link.iconRes,
                        tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                    ),
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
            }
        }
    }
}

@Composable
fun LinksBlockPlaceholder(modifier: Modifier = Modifier) {
    LinksBlockPlaceholderV2(modifier)
}

@Composable
private fun LinksBlockPlaceholderV2(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SubBlockPlaceholderV2()
        SubBlockPlaceholderV2()
        SubBlockPlaceholderV2()
    }
}

@Composable
private fun SubBlockPlaceholderV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(TangemTheme.dimens2.x2),
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
private fun PlaceholderPreviewV2() {
    TangemThemePreviewRedesign {
        Column {
            LinksBlockPlaceholderV2()
            ContentPreviewV2()
        }
    }
}