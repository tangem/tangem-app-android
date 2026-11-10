package com.tangem.features.collectibles.impl.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM.PendingPackUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.ceil
import kotlin.math.floor

/** One pending purchase as a banner; several as a carousel with page dots. */
@Composable
internal fun CollectiblesPendingPacks(packs: ImmutableList<PendingPackUM>, modifier: Modifier = Modifier) {
    when (packs.size) {
        0 -> Unit
        1 -> PendingPackBanner(pack = packs.first(), modifier = modifier.padding(horizontal = 16.dp))
        else -> PendingPacksCarousel(packs = packs, modifier = modifier)
    }
}

@Composable
private fun PendingPacksCarousel(packs: ImmutableList<PendingPackUM>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { packs.size })

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SmoothHeightPager(packs = packs, pagerState = pagerState, modifier = Modifier.fillMaxWidth())
        SpacerH8()
        TangemPagerIndicator(pagerState = pagerState)
    }
}

/** Banners differ in height, so the pager blends the heights of the two pages it is between instead of jumping. */
@Composable
private fun SmoothHeightPager(
    packs: ImmutableList<PendingPackUM>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val pageConstraints = constraints.copy(minHeight = 0)

        val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
            .coerceIn(0f, packs.lastIndex.toFloat())
        val lowerPage = floor(position).toInt().coerceIn(packs.indices)
        val upperPage = ceil(position).toInt().coerceIn(packs.indices)

        fun measureHeight(page: Int): Int = subcompose(slotId = "measure_$page") {
            PendingPackBanner(pack = packs[page], modifier = Modifier.padding(horizontal = 16.dp))
        }.first().measure(pageConstraints).height

        val lowerHeight = measureHeight(lowerPage)
        val upperHeight = if (upperPage == lowerPage) lowerHeight else measureHeight(upperPage)
        val height = lerp(lowerHeight, upperHeight, position - floor(position))

        val pager = subcompose(slotId = "pager") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 8.dp,
                key = { page -> packs[page].txId },
            ) { page ->
                PendingPackBanner(pack = packs[page], modifier = Modifier.padding(horizontal = 16.dp))
            }
        }.first().measure(constraints.copy(minHeight = height, maxHeight = height))

        layout(pager.width, height) { pager.place(0, 0) }
    }
}

@Composable
private fun PendingPackBanner(pack: PendingPackUM, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = pack.title,
        description = pack.description,
        secondaryButton = pack.button,
        slotEnd = when {
            pack.onCloseClick != null -> {
                { TangemMessageBanner.CloseButton(onClick = pack.onCloseClick) }
            }
            pack.icon != null -> {
                {
                    TangemIcon(
                        TangemIconUM.Icon(
                            imageVector = pack.icon,
                            tintReference = { TangemTheme.colors3.icon.primary },
                        ),
                    )
                }
            }
            else -> null
        },
        modifier = modifier,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPendingPacks_Preview() {
    TangemThemePreviewRedesign {
        CollectiblesPendingPacks(
            packs = persistentListOf(
                PendingPackUM(
                    txId = "1",
                    title = stringReference("Your payment is in progress"),
                    description = stringReference("Please wait a bit for the transaction to complete"),
                    icon = Icons.ic_clock_20,
                    onCloseClick = null,
                    button = null,
                ),
                PendingPackUM(
                    txId = "2",
                    title = stringReference("Failed to open pack"),
                    description = stringReference("Our partner has an issue. Please contact support"),
                    icon = null,
                    onCloseClick = {},
                    button = TangemMessageBanner.Button(text = stringReference("Get assistance"), onClick = {}),
                ),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}