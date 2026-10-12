package com.tangem.features.tangempay.card.details

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.*
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.features.tangempay.card.gpay.AddToGoogleWalletButtonState
import com.tangem.features.tangempay.card.gpay.AddToWalletBlockState
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletButton
import com.tangem.features.tangempay.card.gpay.TangemPayAddToWalletBlock
import com.tangem.features.tangempay.card.reissue.TangemPayReissueBlock
import com.tangem.features.tangempay.card.view.DisplayNameState
import com.tangem.features.tangempay.card.view.TangemPayCard
import com.tangem.features.tangempay.card.view.TangemPayCardDetailsUM
import com.tangem.features.tangempay.common.PayContextMenuButton
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.details.impl.R
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tangem.core.ui.R as CoreUiR

private const val CONTENT_FADE_DURATION_MS = 300
private val CardHorizontalPadding = 16.dp
private val CardTopSpacing = 12.dp

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
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            CardPageTopBar(
                items = state.menuItems,
                onBackClick = state.onBackClick,
            )
        },
    ) { contentPadding ->
        val reissueTitle = progressBannerTitleOrNull(cardState = state.cardState)

        if (state.delivery != null) {
            CardDeliveryLayout(
                state = state.delivery,
                cardSection = cardSection,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else if (reissueTitle != null) {
            ReissueCardLayout(
                title = reissueTitle,
                cardSection = cardSection,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(bottom = TangemTheme.dimens.spacing16),
            )
        } else {
            // Material buttons inside the scaffold content can't blur the scaffold's own haze source (Haze skips
            // an ancestor source of the same state), so they sample a separate state backed by the plain background.
            val buttonsHazeState = rememberHazeState()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(TangemTheme.colors3.bg.primary)
                    .hazeSourceTangem(state = buttonsHazeState, zIndex = -1f),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding.copy(
                    bottom = contentPadding.calculateBottomPadding() + TangemTheme.dimens.spacing16,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "Card") {
                    Box(modifier = Modifier.padding(top = CardTopSpacing)) {
                        cardSection()
                    }
                }
                if (
                    state.settings.isNotEmpty() &&
                    state.cardState == TangemPayCardState.Active
                ) {
                    cardPageItem("Settings buttons") {
                        CompositionLocalProvider(LocalHazeState provides buttonsHazeState) {
                            TangemPayCardPageSettingsButtonsBlock(
                                modifier = Modifier.fillMaxWidth(),
                                settings = state.settings,
                            )
                        }
                    }
                }
                cardState(state = state)
            }
        }
    }
}

private fun progressBannerTitleOrNull(cardState: TangemPayCardState): TextReference? {
    return when (cardState) {
        TangemPayCardState.Reissuing -> combinedReference(
            resourceReference(R.string.tangempay_reissue_card_in_progress),
            stringReference(". "),
            resourceReference(R.string.tangempay_reissue_card_in_progress_description),
        )
        TangemPayCardState.Issuing -> combinedReference(
            resourceReference(R.string.tangempay_issuing_new_digital_card_title),
            stringReference(". "),
            resourceReference(R.string.tangempay_reissue_card_in_progress_description),
        )
        TangemPayCardState.Closing -> combinedReference(
            resourceReference(R.string.tangempay_card_page_closing_banner_title),
            stringReference(". "),
            resourceReference(R.string.tangempay_card_page_closing_banner_description),
        )
        TangemPayCardState.Activating -> combinedReference(
            resourceReference(R.string.tangempay_card_page_activating_banner_title),
            stringReference(". "),
            resourceReference(R.string.tangempay_reissue_card_in_progress_description),
        )
        TangemPayCardState.Delivering,
        TangemPayCardState.Active,
        -> null
    }
}

@Composable
private fun ReissueCardLayout(
    title: TextReference,
    modifier: Modifier = Modifier,
    cardSection: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.padding(top = CardTopSpacing)) {
            cardSection()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            TangemPayReissueBlock(title = title)
        }
    }
}

@Composable
private fun CardDeliveryLayout(
    state: TangemPayCardDeliveryUM,
    modifier: Modifier = Modifier,
    cardSection: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.padding(top = CardTopSpacing)) {
            cardSection()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            TangemPayReissueBlock(title = deliveryDescription(email = state.email))
        }
        CardDeliveryButtons(
            onContactSupportClick = state.onContactSupportClick,
            onActivateCardClick = state.onActivateCardClick,
        )
    }
}

@Composable
private fun deliveryDescription(email: String): TextReference {
    val emailStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    return resourceReference(
        id = R.string.tangempay_card_details_delivery_description,
        formatArgs = wrappedList(AnnotatedString(text = email, spanStyle = emailStyle)),
    )
}

@Composable
private fun CardDeliveryButtons(
    onContactSupportClick: () -> Unit,
    onActivateCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TangemPayTestTags.DELIVERY_CONTACT_SUPPORT_BUTTON),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_contact_support),
            onClick = onContactSupportClick,
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TangemPayTestTags.DELIVERY_ACTIVATE_BUTTON),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_card_details_activate),
            onClick = onActivateCardClick,
        )
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
            modifier = modifier.padding(horizontal = CardHorizontalPadding),
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
                    contentPadding = PaddingValues(horizontal = CardHorizontalPadding),
                    pageSpacing = 8.dp,
                    beyondViewportPageCount = 1,
                    key = { controllers[it].cardId },
                ) { page ->
                    CardDetailsPage(controller = controllers[page])
                }

                TangemPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.padding(top = 12.dp),
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
    if (state.cardState != TangemPayCardState.Active) return
    if (state.addToWalletBlockState != null) {
        cardPageItem(key = "GooglePay") {
            TangemPayAddToWalletBlock(
                state = state.addToWalletBlockState,
            )
        }
    }
    if (state.addToGoogleWalletButtonState != null) {
        cardPageItem(key = "GoogleWalletButton") {
            TangemPayAddToGoogleWalletButton(
                onClick = state.addToGoogleWalletButtonState.onClick,
                isLoading = state.addToGoogleWalletButtonState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    cardPageItem(key = "Limit") {
        TangemPayDailyLimitBlock(
            state = state.dailyLimitState,
        )
    }
}

@Composable
private fun CardPageTopBar(
    onBackClick: () -> Unit,
    items: ImmutableList<TangemPayDropDownItemUM>,
    modifier: Modifier = Modifier,
) {
    TangemTopNavigation(
        modifier = modifier,
        startButton = { TangemButton.Back(onClick = onBackClick) },
        endButtonsGroup = {
            PayContextMenuButton(items = items, testTag = TangemPayTestTags.CARD_PAGE_MORE_BUTTON)
        },
    )
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

private fun previewCardDetailsState(
    cardState: TangemPayCardState = TangemPayCardState.Active,
    cardType: TangemPayCardType = TangemPayCardType.VIRTUAL,
): TangemPayCardDetailsUM = TangemPayCardDetailsUM(
    cardState = cardState,
    cardType = cardType,
    number = "•••• •••• •••• 1245",
    numberShort = "··1245",
    cardholderName = "JOHNNY SILVERHAND",
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
    cardImageUrl = null,
    cardBackgroundImageUrl = null,
)

private fun previewDeliveryState(email: String) = TangemPayCardDeliveryUM(
    email = email,
    onContactSupportClick = {},
    onActivateCardClick = {},
)

private fun previewCardPageState(
    cardState: TangemPayCardState = TangemPayCardState.Active,
    addToWalletBlockState: AddToWalletBlockState? = AddToWalletBlockState(onClick = {}, onClickClose = {}),
    addToGoogleWalletButtonState: AddToGoogleWalletButtonState? = null,
    settings: ImmutableList<TangemPayCardPageSetting> = previewSettings(),
    delivery: TangemPayCardDeliveryUM? = null,
) = TangemPayCardPageUM(
    settings = settings,
    onBackClick = {},
    dailyLimitState = TangemPayDailyLimitBlockState.Content(limit = "$5,000", onChangeClick = {}),
    addToWalletBlockState = addToWalletBlockState,
    addToGoogleWalletButtonState = addToGoogleWalletButtonState,
    cardState = cardState,
    menuItems = persistentListOf(),
    delivery = delivery,
)

private fun previewSettings(): ImmutableList<TangemPayCardPageSetting> = persistentListOf(
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.Details,
        title = resourceReference(R.string.details_title),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_visa_card_details_24,
    ),
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.Freeze,
        title = resourceReference(R.string.tangem_pay_freeze_card_freeze),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_freeze_24,
        isLoading = true,
    ),
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.ChangePin,
        title = resourceReference(R.string.tangem_pay_pin_code_title),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_card_pin_24,
        isEnabled = false,
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayCardPageScreen(
            state = previewCardPageState(),
            cardSection = {
                TangemPayCard(
                    state = previewCardDetailsState(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenGoogleWalletPreview() {
    TangemThemePreviewRedesign {
        TangemPayCardPageScreen(
            state = previewCardPageState(
                addToWalletBlockState = null,
                addToGoogleWalletButtonState = AddToGoogleWalletButtonState(onClick = {}, isLoading = false),
            ),
            cardSection = {
                TangemPayCard(
                    state = previewCardDetailsState(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenDeliveryPreview(
    @PreviewParameter(CardDeliveryPreviewProvider::class) delivery: TangemPayCardDeliveryUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCardPageScreen(
            state = previewCardPageState(
                cardState = TangemPayCardState.Delivering,
                addToWalletBlockState = null,
                settings = persistentListOf(),
                delivery = delivery,
            ),
            cardSection = {
                TangemPayCard(
                    state = previewCardDetailsState(
                        cardState = TangemPayCardState.Delivering,
                        cardType = TangemPayCardType.PHYSICAL,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenActivatingPreview() {
    TangemThemePreviewRedesign {
        TangemPayCardPageScreen(
            state = previewCardPageState(
                cardState = TangemPayCardState.Activating,
                addToWalletBlockState = null,
                settings = persistentListOf(),
            ),
            cardSection = {
                TangemPayCard(
                    state = previewCardDetailsState(
                        cardState = TangemPayCardState.Activating,
                        cardType = TangemPayCardType.PHYSICAL,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
    }
}

private class CardDeliveryPreviewProvider : CollectionPreviewParameterProvider<TangemPayCardDeliveryUM>(
    collection = listOf(
        previewDeliveryState(email = "j.silverhand@gmail.com"),
        previewDeliveryState(email = "johnny.silverhand.samurai@protonmail.com"),
    ),
)