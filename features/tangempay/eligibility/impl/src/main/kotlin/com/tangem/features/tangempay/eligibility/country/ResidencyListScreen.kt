package com.tangem.features.tangempay.eligibility.country

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_search_24
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ResidencyListScreen(state: ResidencyListUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = state.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                onClose = state.onCloseClick,
            )
        },
        overlay = {
            if (state.content.isSearchVisible) {
                ResidencySearchFooter(
                    state = state.search,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
    ) { contentPadding ->
        AnimatedContent(
            targetState = state.content,
            contentKey = { content -> content::class },
            modifier = Modifier.fillMaxSize(),
        ) { content ->
            when (content) {
                is ResidencyListUM.Content.Loading -> LoadingContent(contentPadding = contentPadding)
                is ResidencyListUM.Content.Empty -> EmptyContent(contentPadding = contentPadding)
                is ResidencyListUM.Content.Error -> ErrorContent(
                    content = content,
                    contentPadding = contentPadding,
                )
                is ResidencyListUM.Content.Sections -> SectionsContent(
                    content = content,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun SectionsContent(
    content: ResidencyListUM.Content.Sections,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + FOOTER_INSET,
        ),
    ) {
        content.sections.forEach { section ->
            if (section.title != null) {
                item(key = "${section.id}_header") {
                    SectionHeader(title = section.title.resolveReference())
                }
            }
            items(
                items = section.items,
                key = { item -> "${section.id}_${item.id}" },
            ) { item ->
                ResidencyRow(item = item)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = title,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun ResidencyRow(item: ResidencyListUM.ItemUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = item.onClick,
        startSlot = item.flagUrl?.let { url -> { ResidencyFlag(flagUrl = url) } },
        titleSlot = {
            TangemRowText(text = item.name, role = TangemRowTextRole.Title)
        },
        endSlot = if (item.isLoading) {
            {
                TangemLoader(
                    color = TangemTheme.colors3.icon.secondary,
                    size = TangemLoaderSize.X20,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ResidencyFlag(flagUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = flagUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(FLAG_SIZE)
            .clip(CircleShape)
            .background(color = TangemTheme.colors3.bg.opaque.primary),
    )
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            repeat(times = LOADING_ITEMS_COUNT) {
                TangemRow(
                    verticalAlignment = TangemRowVerticalAlignment.Center,
                    startSlot = {
                        TangemShimmer(
                            modifier = Modifier.size(FLAG_SIZE),
                            radius = FLAG_SIZE / 2,
                        )
                    },
                    titleSlot = {
                        TangemShimmer(
                            style = TangemTheme.typography3.body.medium,
                            modifier = Modifier.weight(weight = 1f),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    content: ResidencyListUM.Content.Error,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 64.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X10,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_20),
            onClick = content.onRetryClick,
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_common_error_list_loading),
            textAlign = TextAlign.Center,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

@Composable
private fun EmptyContent(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_search_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = stringResourceSafe(R.string.common_no_results),
            textAlign = TextAlign.Center,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

private val ResidencyListUM.Content.isSearchVisible: Boolean
    get() = this is ResidencyListUM.Content.Sections || this is ResidencyListUM.Content.Empty

private val FLAG_SIZE = 40.dp
private val FOOTER_INSET = 88.dp
private const val LOADING_ITEMS_COUNT = 5

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ResidencyListScreen_Countries() {
    TangemThemePreviewRedesign {
        ResidencyListScreen(state = previewState(content = previewCountries()))
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ResidencyListScreen_Loading() {
    TangemThemePreviewRedesign {
        ResidencyListScreen(state = previewState(content = ResidencyListUM.Content.Loading))
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ResidencyListScreen_Error() {
    TangemThemePreviewRedesign {
        ResidencyListScreen(state = previewState(content = ResidencyListUM.Content.Error(onRetryClick = {})))
    }
}

private fun previewState(content: ResidencyListUM.Content) = ResidencyListUM(
    title = resourceReference(R.string.tangempay_residency_title_country),
    search = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        onQueryChange = {},
        isActive = false,
        onActiveChange = {},
    ),
    content = content,
    onBackClick = null,
    onCloseClick = {},
)

private fun previewCountries() = ResidencyListUM.Content.Sections(
    sections = persistentListOf(
        ResidencyListUM.SectionUM(
            id = "suggested",
            title = resourceReference(R.string.tangempay_residency_suggested_country),
            items = persistentListOf(
                ResidencyListUM.ItemUM(
                    id = "US",
                    name = "United States of America",
                    flagUrl = "https://example.com/flag/us",
                    isLoading = false,
                    onClick = {},
                ),
            ),
        ),
        ResidencyListUM.SectionUM(
            id = "all",
            title = resourceReference(R.string.tangempay_residency_all_country),
            items = persistentListOf(
                ResidencyListUM.ItemUM(
                    id = "AD",
                    name = "Andorra",
                    flagUrl = null,
                    isLoading = false,
                    onClick = {},
                ),
                ResidencyListUM.ItemUM(
                    id = "AL",
                    name = "Albania",
                    flagUrl = "https://example.com/flag/al",
                    isLoading = true,
                    onClick = {},
                ),
                ResidencyListUM.ItemUM(
                    id = "AM",
                    name = "Armenia",
                    flagUrl = "https://example.com/flag/am",
                    isLoading = false,
                    onClick = {},
                ),
            ),
        ),
    ),
)