package com.tangem.features.collectibles.impl.nftdetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.collectibles.impl.nftdetails.ui.state.CollectiblesNftDetailsUM
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.DEFAULT_CARD_ASPECT
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCardTiming
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCase
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.rewardCaseCameraDistance
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.rewardCaseSize
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardProgress
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.persistentListOf

/** The case's frame in the design (Figma `1757:60736`): 402 × 376 dp. */
private const val CASE_ASPECT_RATIO = 402f / 376f

@Composable
internal fun CollectiblesNftDetailsScreen(
    state: CollectiblesNftDetailsUM,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onBuybackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.collectibles_card_details_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = onBackClick,
                onClose = onCloseClick,
            )
        },
        overlay = {
            val buyback = (state as? CollectiblesNftDetailsUM.Content)?.buyback
            if (buyback != null) {
                NftBuybackFooter(
                    buyback = buyback,
                    onBuybackClick = onBuybackClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> footerHeight = size.height },
                )
            }
        },
    ) { contentPadding ->
        val bottomInset = if (state is CollectiblesNftDetailsUM.Content && state.buyback != null) {
            with(LocalDensity.current) { footerHeight.toDp() }
        } else {
            contentPadding.calculateBottomPadding()
        }

        when (state) {
            is CollectiblesNftDetailsUM.Content -> NftDetailsContent(
                state = state,
                contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = bottomInset),
            )
            CollectiblesNftDetailsUM.Loading -> Unit
        }
    }
}

@Composable
private fun NftDetailsContent(state: CollectiblesNftDetailsUM.Content, contentPadding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize()) {
        val caseBackdrop = rememberHazeState()

        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem(state = caseBackdrop)
                .background(TangemTheme.colors3.bg.primary),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding() + 24.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(LocalHazeState provides caseBackdrop) {
                NftCase(state = state)
            }
            Spacer(modifier = Modifier.height(32.dp))
            NftTitle(state = state)
            Spacer(modifier = Modifier.height(24.dp))
            NftDetailRows(rows = state.details)
        }
    }
}

@Composable
private fun NftCase(state: CollectiblesNftDetailsUM.Content) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CASE_ASPECT_RATIO),
        contentAlignment = Alignment.Center,
    ) {
        val cardAspect = state.cardAspect ?: return@BoxWithConstraints
        val caseSize = rewardCaseSize(maxWidth = maxWidth, maxHeight = maxHeight, cardAspect = cardAspect)
        val cameraDistance = rewardCaseCameraDistance(widthPx = constraints.maxWidth)

        Box(modifier = Modifier.size(caseSize)) {
            RewardCase(
                frontImageUrl = state.frontImageUrl,
                backImageUrl = state.backImageUrl,
                timing = RewardCardTiming.Default,
                progress = RewardCardProgress.Settled,
                cameraDistance = cameraDistance,
            )
        }
    }
}

@Composable
private fun NftTitle(state: CollectiblesNftDetailsUM.Content) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.buyback != null) {
            TangemBadge(
                text = state.buyback.timeLeft,
                status = TangemBadge.Status.Warning,
                size = TangemBadge.Size.X6,
                iconStart = TangemIconUM.Icon(Icons.ic_clock_16),
            )
        }
        NftTitleText(title = state.title, subtitle = state.subtitle)
    }
}

@Composable
private fun NftTitleText(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = subtitle,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NftBuybackFooter(
    buyback: CollectiblesNftDetailsUM.BuybackUM,
    onBuybackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemFooter(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = buyback.buttonText,
            iconEnd = TangemIconUM.Icon(Icons.ic_logo_tangem_24),
            onClick = onBuybackClick,
        )
    }
}

@Composable
private fun NftDetailRows(rows: List<CollectiblesNftDetailsUM.DetailRowUM>) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        rows.forEachIndexed { index, row ->
            TangemRow(
                divider = index != rows.lastIndex,
                titleSlot = {
                    Text(
                        text = row.label.resolveReference(),
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
                valueSlot = { TangemRowText(text = row.value, role = TangemRowTextRole.Value) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesNftDetailsScreenPreview() {
    TangemThemePreviewRedesign {
        CollectiblesNftDetailsScreen(
            state = CollectiblesNftDetailsUM.Content(
                frontImageUrl = null,
                backImageUrl = null,
                title = "2000 #236 Tom Brady",
                subtitle = "PSA 9 • $11,000.00",
                details = persistentListOf(
                    CollectiblesNftDetailsUM.DetailRowUM(label = stringReference("Grading company"), value = "PSA"),
                    CollectiblesNftDetailsUM.DetailRowUM(label = stringReference("Grade"), value = "GEM-MT 10"),
                    CollectiblesNftDetailsUM.DetailRowUM(label = stringReference("Grading ID"), value = "#0000000000"),
                    CollectiblesNftDetailsUM.DetailRowUM(label = stringReference("Insured value"), value = "$62.00"),
                ),
                buyback = CollectiblesNftDetailsUM.BuybackUM(
                    timeLeft = stringReference("Buyback is available for 25h 24m"),
                    buttonText = stringReference("Accept buyback for $55.80"),
                ),
                cardAspect = DEFAULT_CARD_ASPECT,
            ),
            onBackClick = {},
            onCloseClick = {},
            onBuybackClick = {},
        )
    }
}