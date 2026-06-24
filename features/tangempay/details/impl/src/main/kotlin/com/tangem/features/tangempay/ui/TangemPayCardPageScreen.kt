package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.*
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.model.controller.TangemPayCardDetailsController
import com.tangem.features.tangempay.ui.components.PayContextMenuBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tangem.core.ui.R as CoreUiR

private const val CONTENT_FADE_DURATION_MS = 300

@Composable
internal fun TangemPayCardPageScreen(
    state: TangemPayCardPageUM,
    cardControllers: ImmutableList<TangemPayCardDetailsController>,
    selectedCardId: String,
    onCardSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemPayCardPageScreen(
        state = state,
        cardSection = {
            TangemPayCardSwipePager(
                controllers = cardControllers,
                selectedCardId = selectedCardId,
                onCardSelect = onCardSelect,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun TangemPayCardPageScreen(
    state: TangemPayCardPageUM,
    modifier: Modifier = Modifier,
    cardSection: @Composable () -> Unit,
) {
    val isRedesignEnabled = LocalVisaRedesignEnabled.current
    Scaffold(
        modifier = modifier,
        topBar = {
            CardPageTopBar(
                items = state.menuItems,
                onBackClick = state.onBackClick,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        containerColor = if (isRedesignEnabled) {
            TangemTheme.colors3.bg.primary
        } else {
            TangemTheme.colors.background.secondary
        },
    ) { scaffoldPaddings ->
        TangemPayCardPageContent(
            scaffoldPaddings = scaffoldPaddings,
            state = state,
            isRedesignEnabled = isRedesignEnabled,
            cardSection = cardSection,
        )
    }
}

@Composable
private fun TangemPayCardPageContent(
    state: TangemPayCardPageUM,
    scaffoldPaddings: PaddingValues,
    isRedesignEnabled: Boolean,
    cardSection: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPaddings),
    ) {
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
        val isCardInPendingState = state.cardState.isPendingState()
        LazyColumn(
            modifier = Modifier
                .then(
                    if (isCardInPendingState) {
                        Modifier.wrapContentHeight(align = Alignment.Top)
                    } else {
                        Modifier.fillMaxHeight()
                    },
                ),
            contentPadding = PaddingValues(
                bottom = if (isCardInPendingState) {
                    0.dp
                } else {
                    TangemTheme.dimens.spacing16 + bottomBarHeight
                },
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (isRedesignEnabled) 0.dp else TangemTheme.dimens.spacing16,
            ),
        ) {
            item(key = "Card") {
                Box(modifier = Modifier.padding(top = TangemTheme.dimens.spacing8)) {
                    cardSection()
                }
            }
            if (isRedesignEnabled &&
                state.settingsV2.isNotEmpty() &&
                state.cardState == TangemPayCardState.Active
            ) {
                cardPageItem("Settings buttons") {
                    TangemPayCardPageSettingsButtonsBlock(
                        modifier = Modifier.fillMaxWidth(),
                        settings = state.settingsV2,
                    )
                }
            }
            cardState(state)
        }
        if (isCardInPendingState) {
            val description = getPendingStateDescription(state.cardState)
            PendingCardStateContent(
                description = description,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TangemTheme.dimens.spacing48)
                    .padding(bottom = bottomBarHeight),
            )
        }
    }
}

/**
 * Renders the card visual(s). With several cards they are wrapped in a [HorizontalPager] so the user
 * can swipe to change the management context; the neighbouring cards peek at the screen edges and a
 * dots indicator below shows the position. Each page collects its own controller's state so only the
 * changed page recomposes. On settle the screen reports the new page via [onCardSelect].
 */
@Composable
private fun TangemPayCardSwipePager(
    controllers: ImmutableList<TangemPayCardDetailsController>,
    selectedCardId: String,
    onCardSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        controllers.isEmpty() -> Unit
        controllers.size == 1 -> CardDetailsPage(
            controller = controllers.first(),
            modifier = modifier.padding(horizontal = 16.dp),
        )
        else -> {
            val initialPage = controllers.indexOfFirst { it.cardId == selectedCardId }.coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = initialPage) { controllers.size }

            LaunchedEffect(pagerState, controllers) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect(onCardSelect)
            }

            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    // Side padding keeps the current card centered while the neighbours peek at the edges.
                    contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing32),
                    pageSpacing = TangemTheme.dimens.spacing8,
                    beyondViewportPageCount = 1,
                    key = { controllers[it].cardId },
                ) { page ->
                    CardDetailsPage(controller = controllers[page])
                }

                TangemPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                )
            }
        }
    }
}

@Composable
private fun CardDetailsPage(controller: TangemPayCardDetailsController, modifier: Modifier = Modifier) {
    val cardDetailsState by controller.uiState.collectAsStateWithLifecycle()
    TangemPayCard(state = cardDetailsState, modifier = modifier)
}

private fun LazyListScope.cardState(state: TangemPayCardPageUM) {
    when (state.cardState) {
        TangemPayCardState.Active -> {
            if (state.addToWalletBlockState != null) {
                cardPageItem(key = "GooglePay") {
                    TangemPayAddToWalletBlock(state = state.addToWalletBlockState)
                }
            }
            cardPageItem(key = "Limit") {
                TangemPayDailyLimitBlock(state = state.dailyLimitState)
            }
            if (state.dailyLimitState is TangemPayDailyLimitBlockState.Error) {
                cardPageItem(key = "LimitError") {
                    TangemPayDailyLimitErrorBlock()
                }
            }
            cardPageItem(key = "Settings") {
                TangemPayCardPageSettingsBlock(settings = state.settings)
            }
        }
        /** see [PendingCardStateContent] */
        TangemPayCardState.Reissuing,
        TangemPayCardState.Closing,
        TangemPayCardState.Issuing,
        -> Unit
    }
}

@Composable
private fun TangemPayCardPageSettingsBlock(
    settings: ImmutableList<TangemPayCardPageSetting>,
    modifier: Modifier = Modifier,
) {
    if (LocalVisaRedesignEnabled.current) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            ),
    ) {
        Text(
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing4,
            ),
            text = stringResourceSafe(R.string.tangempay_card_page_settings_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        settings.fastForEach { item ->
            TangemPayCardPageSettingRow(
                item = item,
                onClick = item.onSettingClick,
            )
        }
    }
}

@Composable
private fun TangemPayCardPageSettingRow(
    item: TangemPayCardPageSetting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(TangemTheme.dimens.spacing12)
            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = item.title.resolveReference(),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun CardPageTopBar(
    onBackClick: () -> Unit,
    items: ImmutableList<TangemPayDropDownItemUM>,
    modifier: Modifier = Modifier,
) {
    if (LocalVisaRedesignEnabled.current) {
        var isDropdownMenuShown by rememberSaveable { mutableStateOf(false) }
        TangemTopBar(
            modifier = modifier.statusBarsPadding(),
            startContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_back_28),
                    onClick = onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
            endContent = {
                Box {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_more_default_24),
                        onClick = { isDropdownMenuShown = true },
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                    PayContextMenuBlock(
                        items = items,
                        onMenuDismiss = { isDropdownMenuShown = false },
                        isDropdownMenuShown = isDropdownMenuShown,
                    )
                }
            },
        )
    } else {
        AppBarWithBackButton(
            modifier = modifier.statusBarsPadding(),
            onBackClick = onBackClick,
        )
    }
}

private fun LazyListScope.cardPageItem(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit,
) {
    item(
        key = key,
        contentType = contentType,
    ) {
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(CONTENT_FADE_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(CONTENT_FADE_DURATION_MS)),
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun PendingCardStateContent(description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            TangemTheme.dimens.spacing12,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.opaque.primary)
                .padding(TangemTheme.dimens.spacing10)
                .size(TangemTheme.dimens.size20),
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
        Text(
            text = description,
            textAlign = TextAlign.Center,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Composable
private fun getPendingStateDescription(state: TangemPayCardState): String {
    val textFragments = when (state) {
        TangemPayCardState.Issuing -> {
            listOf(
                resourceReference(R.string.tangempay_issuing_new_digital_card_title),
                resourceReference(R.string.tangempay_reissue_card_in_progress_description),
            )
        }
        TangemPayCardState.Reissuing -> {
            listOf(
                resourceReference(R.string.tangempay_reissue_card_in_progress),
                resourceReference(R.string.tangempay_reissue_card_in_progress_description),
            )
        }
        TangemPayCardState.Closing -> {
            listOf(
                resourceReference(R.string.tangempay_card_page_closing_banner_title),
                resourceReference(R.string.tangempay_card_page_closing_banner_description),
            )
        }
        else -> emptyList()
    }.map { it.resolveReference() }
    return textFragments.joinToString(". ")
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenPreviewV1() {
    TangemThemePreview {
        TangemPayCardPageScreen(
            state = TangemPayCardPageUM.stub(),
            cardSection = {
                TangemPayCard(
                    state = previewCardDetailsState(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
    }
}

private fun previewCardDetailsState(): TangemPayCardDetailsUM = TangemPayCardDetailsUM(
    number = "•••• •••• •••• 1245",
    numberShort = "··1245",
    expiry = "••/••",
    cvv = "•••",
    onCopy = { _, _ -> },
    onClick = {},
    cardFrozenState = TangemPayCardFrozenState.Unfrozen,
    displayNameState = DisplayNameState.Display(
        displayName = "Tangem Pay Card",
        onClick = {},
        isEditingEnabled = false,
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenPreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(
            LocalRedesignEnabled provides true,
            LocalVisaRedesignEnabled provides true,
        ) {
            TangemPayCardPageScreen(
                state = TangemPayCardPageUM.stub(),
                cardSection = {
                    TangemPayCard(
                        state = previewCardDetailsState(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                },
            )
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PendingCardStateContentPreview() {
    TangemThemePreviewRedesign {
        PendingCardStateContent(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            description = getPendingStateDescription(TangemPayCardState.Issuing),
        )
    }
}