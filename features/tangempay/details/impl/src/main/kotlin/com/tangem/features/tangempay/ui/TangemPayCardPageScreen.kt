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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.components.cardDetails.PreviewTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import kotlinx.collections.immutable.ImmutableList

private const val CONTENT_FADE_DURATION_MS = 300

@Composable
internal fun TangemPayCardPageScreen(
    state: TangemPayCardPageUM,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
    cardDetailsState: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    val isRedesignEnabled = LocalRedesignEnabled.current
    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
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
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddings),
            contentPadding = PaddingValues(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16 + bottomBarHeight,
            ),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            item(key = "Card") {
                cardDetailsBlockComponent.CardDetailsBlockContent(
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                    state = cardDetailsState,
                )
            }
            if (isRedesignEnabled && state.settingsV2.isNotEmpty()) {
                cardPageItem("Settings buttons") {
                    TangemPayCardPageSettingsButtonsBlock(
                        modifier = Modifier.fillMaxWidth(),
                        settings = state.settingsV2,
                    )
                }
            }
            if (state.isReissueInProgress) {
                cardPageItem(key = "Reissue") {
                    TangemPayReplacingCardBlock()
                }
            } else {
                if (state.addToWalletBlockState != null) {
                    cardPageItem(key = "GooglePay") {
                        TangemPayAddToWalletBlock(state = state.addToWalletBlockState)
                    }
                }
                cardPageItem(key = "Limit") {
                    TangemPayDailyLimitBlock(state = state.dailyLimitState)
                }
                if (state.dailyLimitState == TangemPayDailyLimitBlockState.Error) {
                    cardPageItem(key = "LimitError") {
                        TangemPayDailyLimitErrorBlock()
                    }
                }
                cardPageItem(key = "Settings") {
                    TangemPayCardPageSettingsBlock(settings = state.settings)
                }
            }
        }
    }
}

@Composable
private fun TangemPayCardPageSettingsBlock(
    settings: ImmutableList<TangemPayCardPageSetting>,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) return
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
            content()
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenPreviewV1() {
    TangemThemePreview {
        TangemPayCardPageScreen(
            state = TangemPayCardPageUM.stub(),
            cardDetailsBlockComponent = PreviewTangemPayCardDetailsBlockComponent(
                TangemPayCardDetailsUM(
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
                        isEditingEnabled = true,
                    ),
                ),
            ),
            cardDetailsState = TangemPayCardDetailsUM(
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
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageScreenPreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            TangemPayCardPageScreen(
                state = TangemPayCardPageUM.stub(),
                cardDetailsBlockComponent = PreviewTangemPayCardDetailsBlockComponent(
                    TangemPayCardDetailsUM(
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
                            isEditingEnabled = true,
                        ),
                    ),
                ),
                cardDetailsState = TangemPayCardDetailsUM(
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
                ),
            )
        }
    }
}