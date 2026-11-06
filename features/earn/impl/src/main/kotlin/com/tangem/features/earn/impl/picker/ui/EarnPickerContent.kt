package com.tangem.features.earn.impl.picker.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.fake.fakeSummary
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.impl.picker.model.EarnPickerConverter
import com.tangem.features.earn.impl.picker.model.EarnPickerCta
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM.ContentUM
import com.tangem.features.earn.impl.ui.EarnDetailRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** The page is the haze source: the calculator's horizon menu blurs what is behind it. */
@Composable
internal fun EarnPickerContent(state: EarnPickerUM, modifier: Modifier = Modifier, calculator: @Composable () -> Unit) {
    val listState = rememberLazyListState()
    var heroHeightPx by remember { mutableIntStateOf(0) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    var tabsHeightPx by remember { mutableIntStateOf(0) }
    var footerHeightPx by remember { mutableIntStateOf(0) }

    val sections = (state.content as? ContentUM.Ready)?.sections ?: persistentListOf()
    val firstSectionIndex = FIRST_PAGE_ITEMS + if (state.products.isNotEmpty()) 1 else 0

    val barProgress by remember {
        derivedStateOf {
            listState.barProgress(heroHeightPx = heroHeightPx.toFloat(), barHeightPx = barHeightPx.toFloat())
        }
    }
    val isTabsPinned by remember(sections, firstSectionIndex) {
        derivedStateOf {
            sections.isNotEmpty() &&
                listState.isItemAbove(TABS_KEY, firstSectionIndex - 1, barHeightPx.toFloat())
        }
    }
    val tabs = rememberSectionTabs(
        listState = listState,
        sections = sections,
        firstSectionIndex = firstSectionIndex,
        headerHeightPx = { barHeightPx + tabsHeightPx },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(),
            contentPadding = PaddingValues(bottom = with(LocalDensity.current) { footerHeightPx.toDp() }),
        ) {
            item(key = "hero") { EarnPickerHero(modifier = Modifier.onSizeChanged { heroHeightPx = it.height }) }
            item(key = "title") { EarnPickerTitle(title = state.title, subtitle = state.subtitle) }
            if (state.products.isNotEmpty()) {
                item(key = "products") { EarnProductCards(products = state.products) }
            }
            content(state = state, calculator = calculator) { tabsItem(tabs) { tabsHeightPx = it } }
        }
        PinnedHeader(
            barTitle = state.barTitle,
            barProgress = barProgress,
            tabs = tabs.takeIf { isTabsPinned },
            onClose = state.onClose,
            onTabsHeightChange = { tabsHeightPx = it },
            onBarHeightChange = { barHeightPx = it },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Footer(
            cta = state.cta,
            hint = state.ctaHint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeightPx = it.height },
        )
    }
}

private fun LazyListScope.content(
    state: EarnPickerUM,
    calculator: @Composable () -> Unit,
    tabsItem: LazyListScope.() -> Unit,
) {
    when (val content = state.content) {
        ContentUM.Loading -> pageItem(key = "loading") { LoadingBody() }
        is ContentUM.Unavailable -> pageItem(key = "unavailable") {
            UnavailableBody(onRetryClick = content.onRetryClick)
        }
        is ContentUM.Ready -> {
            pageItem(key = "description", bottom = if (content.sections.isEmpty()) SECTION_GAP else 0.dp) {
                Description(description = content.description, incentive = content.incentive)
            }
            if (content.sections.isNotEmpty()) tabsItem()
            content.sections.forEach { section ->
                pageItem(key = section.name) {
                    when (section) {
                        EarnPickerTab.Calculator -> CalculatorSection(calculator = calculator)
                        EarnPickerTab.Benefits -> BenefitsSection(content = content)
                        EarnPickerTab.Details -> DetailsSection(content = content)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.tabsItem(tabs: ImmutableList<TangemTabItemUM>, onHeightChange: (Int) -> Unit) {
    item(key = TABS_KEY) {
        Column {
            Box(
                modifier = Modifier
                    .onSizeChanged { onHeightChange(it.height) }
                    .padding(vertical = TABS_VERTICAL_PADDING),
            ) {
                TangemTabNavigation(tabs = tabs, contentPadding = PaddingValues(horizontal = PAGE_MARGIN))
            }
            Spacer(modifier = Modifier.height(TABS_VERTICAL_PADDING))
        }
    }
}

private fun LazyListScope.pageItem(key: String, bottom: Dp = SECTION_GAP, content: @Composable () -> Unit) {
    item(key = key) {
        Box(modifier = Modifier.padding(bottom = bottom)) { content() }
    }
}

@Composable
private fun rememberSectionTabs(
    listState: LazyListState,
    sections: ImmutableList<EarnPickerTab>,
    firstSectionIndex: Int,
    headerHeightPx: () -> Int,
): ImmutableList<TangemTabItemUM> {
    val scope = rememberCoroutineScope()
    val active by remember(sections, firstSectionIndex) {
        derivedStateOf { listState.activeSection(sections, firstSectionIndex, headerHeightPx().toFloat()) }
    }

    return remember(sections, active, firstSectionIndex) {
        sections.mapIndexed { position, section ->
            TangemTabItemUM.Content(
                id = section.name,
                label = EarnPickerTexts.tab(section),
                isSelected = section == active,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(
                            index = firstSectionIndex + position,
                            scrollOffset = -headerHeightPx(),
                        )
                    }
                },
            )
        }.toImmutableList()
    }
}

@Suppress("LongParameterList")
@Composable
private fun PinnedHeader(
    barTitle: TextReference,
    barProgress: Float,
    tabs: ImmutableList<TangemTabItemUM>?,
    onClose: () -> Unit,
    onBarHeightChange: (Int) -> Unit,
    onTabsHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleAlpha = ((barProgress - TITLE_FADE_START) / (1f - TITLE_FADE_START)).coerceIn(0f, 1f)
    val pageColor = TangemTheme.colors3.bg.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawHeaderFade(
                    color = pageColor,
                    alpha = barProgress,
                    overshoot = if (tabs != null) FADE_OVERSHOOT else 0.dp,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onBarHeightChange(it.height) }
                .statusBarsPadding()
                .padding(bottom = BAR_BOTTOM_PADDING)
                .heightIn(min = BAR_HEIGHT),
        ) {
            Text(
                text = barTitle.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = titleAlpha
                        translationY = (1f - titleAlpha) * TITLE_SHIFT.toPx()
                    },
            )
            TangemButton.Close(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                onClick = onClose,
            )
        }
        if (tabs != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onTabsHeightChange(it.height) }
                    .padding(vertical = TABS_VERTICAL_PADDING),
            ) {
                TangemTabNavigation(tabs = tabs, contentPadding = PaddingValues(horizontal = PAGE_MARGIN))
            }
        }
    }
}

private fun DrawScope.drawHeaderFade(color: Color, alpha: Float, overshoot: Dp) {
    val total = size.height + overshoot.toPx()
    val solid = color.copy(alpha = FADE_ALPHA * alpha)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to solid,
                (total - FADE_GRADIENT_HEIGHT.toPx()) / total to solid,
                1f to Color.Transparent,
            ),
            startY = 0f,
            endY = total,
        ),
        size = Size(width = size.width, height = total),
    )
}

/** 0 while the hero fills the top, 1 once it is under the bar. */
private fun LazyListState.barProgress(heroHeightPx: Float, barHeightPx: Float): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (heroHeightPx <= 0f) return 0f
    val end = heroHeightPx - barHeightPx
    val start = end - barHeightPx.coerceAtLeast(1f)
    return ((firstVisibleItemScrollOffset - start) / (end - start)).coerceIn(0f, 1f)
}

private fun LazyListState.isItemAbove(key: String, index: Int, limitPx: Float): Boolean {
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    return if (info != null) info.offset <= limitPx else firstVisibleItemIndex > index
}

/** The last section that has reached the pinned header; the last one when the page cannot scroll further. */
private fun LazyListState.activeSection(
    sections: List<EarnPickerTab>,
    firstSectionIndex: Int,
    headerBottomPx: Float,
): EarnPickerTab? {
    if (sections.isEmpty()) return null
    if (canScrollBackward && !canScrollForward) return sections.last()

    return sections.withIndex()
        .lastOrNull { (position, section) -> isItemAbove(section.name, firstSectionIndex + position, headerBottomPx) }
        ?.value
        ?: sections.first()
}

@Composable
private fun Description(description: TextReference, incentive: TangemBadgeUM?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_MARGIN, end = PAGE_MARGIN, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        incentive?.let { badge ->
            TangemBadge(
                text = badge.text,
                variant = badge.variant,
                status = badge.status,
                size = badge.size,
                iconEnd = badge.iconEnd,
            )
        }
        Text(
            text = description.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun CalculatorSection(calculator: @Composable () -> Unit) {
    Section(title = EarnPickerTexts.calculatorSection) {
        Box(
            modifier = Modifier
                .padding(horizontal = PAGE_MARGIN)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors3.bg.secondary)
                .padding(top = CALCULATOR_TOP_PADDING),
        ) {
            calculator()
        }
    }
}

@Composable
private fun BenefitsSection(content: ContentUM.Ready) {
    Section(title = EarnPickerTexts.benefitsSection) {
        Column(
            modifier = Modifier.padding(start = PAGE_MARGIN, end = PAGE_MARGIN, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content.benefits.forEach { benefit -> EarnBenefitRow(benefit = benefit) }
        }
    }
}

/** The rows keep 8 dp of the page margin and 16 dp of their own: their text lines up with the heading. */
@Composable
private fun DetailsSection(content: ContentUM.Ready) {
    Section(title = EarnPickerTexts.detailsSection) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            content.details.forEachIndexed { index, row ->
                EarnDetailRow(state = row, divider = index < content.details.lastIndex)
            }
        }
    }
}

@Composable
private fun Section(title: TextReference, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(horizontal = PAGE_MARGIN, vertical = 12.dp),
        )
        content()
    }
}

@Composable
private fun LoadingBody() {
    ProvideTangemShimmer {
        Column(
            modifier = Modifier.padding(horizontal = PAGE_MARGIN),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TangemShimmer(style = TangemTheme.typography3.subheading.medium, modifier = Modifier.fillMaxWidth())
            TangemShimmer(modifier = Modifier.fillMaxWidth().height(LOADING_BODY_HEIGHT))
        }
    }
}

@Composable
private fun UnavailableBody(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_MARGIN, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = EarnPickerTexts.unavailable.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            text = EarnPickerTexts.retry,
            onClick = onRetryClick,
        )
    }
}

@Composable
private fun Footer(cta: TangemButtonUM, hint: TextReference?, modifier: Modifier = Modifier) {
    val pageColor = TangemTheme.colors3.bg.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { drawFooterFade(color = pageColor) }
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        hint?.let { hintText ->
            Text(
                text = hintText.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            text = cta.text,
            isEnabled = cta.isEnabled,
            isDimmed = cta.isDimmed,
            onClick = cta.onClick,
        )
    }
}

private fun DrawScope.drawFooterFade(color: Color) {
    val top = FOOTER_FADE_OVERSHOOT.toPx()
    val total = size.height + top
    val ramp = FOOTER_RAMP_HEIGHT.toPx() / total
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                ramp * FOOTER_RAMP_STOP_1 to color.copy(alpha = FOOTER_RAMP_ALPHA_1),
                ramp * FOOTER_RAMP_STOP_2 to color.copy(alpha = FOOTER_RAMP_ALPHA_2),
                ramp to color.copy(alpha = FADE_ALPHA),
                1f to color.copy(alpha = FADE_ALPHA),
            ),
            startY = 0f,
            endY = total,
        ),
        topLeft = Offset(x = 0f, y = -top),
        size = Size(width = size.width, height = total),
    )
}

private val LOADING_BODY_HEIGHT = 240.dp

private val BAR_HEIGHT = 44.dp
private val BAR_BOTTOM_PADDING = 16.dp

private const val FADE_ALPHA = 0.95f
private val FADE_GRADIENT_HEIGHT = 40.dp
private val FADE_OVERSHOOT = 2.dp
private val FOOTER_FADE_OVERSHOOT = 16.dp
private val FOOTER_RAMP_HEIGHT = 24.dp
private const val FOOTER_RAMP_STOP_1 = 0.3f
private const val FOOTER_RAMP_STOP_2 = 0.697f
private const val FOOTER_RAMP_ALPHA_1 = 0.4f
private const val FOOTER_RAMP_ALPHA_2 = 0.85f

private val SECTION_GAP = 24.dp
private val TABS_VERTICAL_PADDING = 12.dp
private val CALCULATOR_TOP_PADDING = 40.dp
private val TITLE_SHIFT = 8.dp
private const val TITLE_FADE_START = 0.4f
private const val TABS_KEY = "tabs"

/** The hero, the title block and the description come before the first section; the tab row is one more item. */
private const val FIRST_PAGE_ITEMS = 4

@Composable
private fun PreviewContent(state: EarnPickerUM) {
    TangemThemePreviewRedesign {
        EarnPickerContent(
            state = state,
            calculator = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LOADING_BODY_HEIGHT),
                )
            },
        )
    }
}

private object PreviewData {

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)
    private val converter = EarnPickerConverter(
        registry = EarnProductsRegistry(products = setOf(lending, vault), rateSources = emptySet()),
        cta = EarnPickerCta(),
    )
    private val callbacks = EarnPickerConverter.Callbacks(
        onProductClick = {},
        onActionClick = { _, _ -> },
        onBlocked = {},
        onAddFunds = {},
        onRetryClick = {},
        onInfoClick = {},
        onLinkClick = {},
        onClose = {},
    )
    private val summaries = listOf(
        fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.None),
        fakeSummary(FakeEarnProductPreset.Vault, FakeEarnPosition.None),
    )

    fun ready(
        position: FakeEarnPosition = FakeEarnPosition.None,
        ethBalance: BigDecimal = BigDecimal.ONE,
    ): EarnPickerUM = converter.convert(
        product = lending,
        state = fakeReadyState(FakeEarnProductPreset.Lending, position),
        summaries = summaries,
        selectedId = lending.id,
        ethBalance = ethBalance,
        callbacks = callbacks,
    )

    fun loading(): EarnPickerUM = converter.initial(onClose = {})

    fun unavailable(): EarnPickerUM = converter.unavailable(onRetryClick = {}, onClose = {})
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnPickerContentReadyPreview() {
    PreviewContent(state = PreviewData.ready())
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnPickerContentLoadingPreview() {
    PreviewContent(state = PreviewData.loading())
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnPickerContentUnavailablePreview() {
    PreviewContent(state = PreviewData.unavailable())
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnPickerContentDimmedButtonPreview() {
    PreviewContent(state = PreviewData.ready(position = FakeEarnPosition.Unknown))
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnPickerContentAddFundsPreview() {
    PreviewContent(state = PreviewData.ready(ethBalance = BigDecimal.ZERO))
}