package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.common.ui.onboarding.OnboardingHero
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20

private const val HERO_TINT_START_Y = 380f
private const val HERO_TINT_END_Y = 532f

@Composable
internal fun TangemPayUnifiedOnboardingLayout(
    isGetStartedLoading: Boolean,
    onGetStartedClick: () -> Unit,
    onTermsClick: () -> Unit,
    onTermsAndFeesClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }
    val isTopBarScrimShown by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                fadeEnabled = isTopBarScrimShown,
                endButtonsGroup = {
                    TangemButton(
                        variant = TangemButton.Variant.Ghost,
                        size = TangemButton.Size.X9,
                        iconStart = TangemIconUM.Icon(imageVector = Icons.ic_document_20),
                        contentDescription = stringResourceSafe(R.string.tangem_pay_terms_fees_limits),
                        onClick = onTermsClick,
                    )
                },
                endButton = { TangemButton.Close(onClick = onCloseClick) },
            )
        },
        overlay = {
            TangemFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
                extraTop = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResourceSafe(R.string.tangempay_unified_onboarding_legal),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                },
            ) {
                TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    TangemButton(
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.tangempay_unified_onboarding_terms_button),
                        onClick = onTermsAndFeesClick,
                    )
                    TangemButton(
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.common_get_started),
                        isLoading = isGetStartedLoading,
                        onClick = onGetStartedClick,
                    )
                }
            }
        },
    ) { contentPadding ->
        UnifiedOnboardingContent(
            scrollState = scrollState,
            contentPadding = contentPadding,
            trailingSpace = with(LocalDensity.current) { footerHeight.toDp() },
        )
    }
}

@Composable
private fun UnifiedOnboardingContent(scrollState: ScrollState, contentPadding: PaddingValues, trailingSpace: Dp) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        OnboardingHero(
            imageResId = R.drawable.ill_tangem_pay_onboarding_bg,
            tintStartY = HERO_TINT_START_Y,
            tintEndY = HERO_TINT_END_Y,
            foregroundImageResId = R.drawable.img_hot_wallet_onboarding,
        ) {
            OnboardingHeadline(
                title = resourceReference(R.string.tangempay_title),
                subtitle = resourceReference(R.string.tangempay_unified_onboarding_description),
            )
            Spacer(modifier = Modifier.height(24.dp))
            UnifiedOnboardingSections()
        }
        Spacer(modifier = Modifier.height(trailingSpace + 48.dp))
    }
}

@Composable
private fun UnifiedOnboardingSections() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Section(header = stringResourceSafe(R.string.tangempay_unified_onboarding_payment_account_header)) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeatureCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    caption = stringResourceSafe(R.string.tangempay_unified_onboarding_cashback_caption),
                    text = highlightedArgText(
                        formatResId = R.string.tangempay_unified_onboarding_cashback_title,
                        dimmedArg = stringResourceSafe(R.string.tangempay_unified_onboarding_cashback_subtitle),
                    ),
                )
                FeatureCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    caption = stringResourceSafe(R.string.tangempay_unified_onboarding_payment_account_caption),
                    text = highlightedArgText(
                        formatResId = R.string.tangempay_unified_onboarding_payment_account_title,
                        dimmedArg = stringResourceSafe(
                            R.string.tangempay_unified_onboarding_payment_account_subtitle_android,
                        ),
                    ),
                )
            }
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                caption = stringResourceSafe(R.string.tangempay_unified_onboarding_fees_caption),
                text = highlightedArgText(
                    formatResId = R.string.tangempay_unified_onboarding_fees_title,
                    dimmedArg = stringResourceSafe(R.string.tangempay_unified_onboarding_fees_subtitle),
                ),
            )
        }
        Section(header = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_header)) {
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                caption = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_caption),
                text = highlightedArgText(
                    formatResId = R.string.tangempay_unified_onboarding_money_account_title,
                    dimmedArg = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_subtitle),
                ),
            )
        }
        Section(header = stringResourceSafe(R.string.tangempay_unified_onboarding_everything_header)) {
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                caption = stringResourceSafe(R.string.tangempay_unified_onboarding_plan_caption),
                text = highlightedArgText(
                    formatResId = R.string.tangempay_unified_onboarding_plan_title,
                    dimmedArg = stringResourceSafe(R.string.tangempay_unified_onboarding_plan_subtitle),
                ),
            )
        }
    }
}

@Composable
private fun Section(header: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            text = header,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FeatureCard(caption: String, text: AnnotatedString, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = caption,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Text(
            text = text,
            style = TangemTheme.typography3.heading.small,
        )
    }
}

@Composable
private fun highlightedArgText(@StringRes formatResId: Int, dimmedArg: String): AnnotatedString {
    val text = stringResourceSafe(formatResId, dimmedArg)
    val dimmedColor = TangemTheme.colors3.text.secondary
    return buildAnnotatedString {
        withStyle(SpanStyle(color = TangemTheme.colors3.text.primary)) { append(text) }
        val start = text.indexOf(dimmedArg)
        if (start >= 0) addStyle(SpanStyle(color = dimmedColor), start, start + dimmedArg.length)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayUnifiedOnboardingLayoutPreview() {
    TangemThemePreviewRedesign {
        TangemPayUnifiedOnboardingLayout(
            isGetStartedLoading = false,
            onGetStartedClick = {},
            onTermsClick = {},
            onTermsAndFeesClick = {},
            onCloseClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}