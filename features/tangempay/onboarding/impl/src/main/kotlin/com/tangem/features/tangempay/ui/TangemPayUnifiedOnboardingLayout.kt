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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.common.ui.onboarding.OnboardingHero
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24

private const val HERO_TINT_START_Y = 380f
private const val HERO_TINT_END_Y = 532f

private const val FRAME_WIDTH = 402f
private const val VIDEO_FADE_TOP = 294f
private const val VIDEO_FADE_BOTTOM = 555f
private const val VIDEO_CONTENT_TOP = 442f

@Composable
internal fun TangemPayUnifiedOnboardingLayout(
    video: VideoSurfaceState?,
    isGetStartedLoading: Boolean,
    onGetStartedClick: () -> Unit,
    onTermsClick: () -> Unit,
    onCloseClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }
    val isTopBarScrimShown by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            if (video == null) {
                UnifiedOnboardingTopBar(
                    isBackgroundBlurred = true,
                    isFadeEnabled = isTopBarScrimShown,
                    onTermsClick = onTermsClick,
                    onCloseClick = onCloseClick,
                )
            } else {
                ProvideHaze {
                    UnifiedOnboardingTopBar(
                        isBackgroundBlurred = false,
                        isFadeEnabled = isTopBarScrimShown,
                        onTermsClick = onTermsClick,
                        onCloseClick = onCloseClick,
                    )
                }
            }
        },
        overlay = {
            TangemFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_get_started),
                    iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
                    isLoading = isGetStartedLoading,
                    onClick = onGetStartedClick,
                )
            }
        },
    ) { contentPadding ->
        val trailingSpace = with(LocalDensity.current) { footerHeight.toDp() }
        if (video == null) {
            UnifiedOnboardingContent(
                scrollState = scrollState,
                contentPadding = contentPadding,
                trailingSpace = trailingSpace,
            )
        } else {
            TangemPayBundleBackdrop(video = video)
            UnifiedOnboardingVideoContent(
                scrollState = scrollState,
                contentPadding = contentPadding,
                trailingSpace = trailingSpace,
            )
        }
    }
}

@Composable
private fun UnifiedOnboardingTopBar(
    isBackgroundBlurred: Boolean,
    isFadeEnabled: Boolean,
    onTermsClick: () -> Unit,
    onCloseClick: (() -> Unit)?,
) {
    TangemTopNavigation(
        blurBackground = isBackgroundBlurred,
        fadeEnabled = isFadeEnabled,
        endButtonsGroup = {
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                size = TangemButton.Size.X9,
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_document_20),
                contentDescription = stringResourceSafe(R.string.tangem_pay_terms_fees_limits),
                onClick = onTermsClick,
            )
        },
        endButton = onCloseClick?.let { onClick -> { TangemButton.Close(onClick = onClick) } },
    )
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
            UnifiedOnboardingBody()
        }
        Spacer(modifier = Modifier.height(trailingSpace + 48.dp))
    }
}

@Composable
private fun UnifiedOnboardingVideoContent(
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    trailingSpace: Dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    val backgroundColor = TangemTheme.colors3.bg.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            )
            .drawWithCache {
                val scale = size.width / FRAME_WIDTH
                val brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to backgroundColor.copy(alpha = 0f),
                        0.5f to backgroundColor.copy(alpha = 0.5f),
                        1f to backgroundColor,
                    ),
                    startY = VIDEO_FADE_TOP * scale,
                    endY = VIDEO_FADE_BOTTOM * scale,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FRAME_WIDTH / VIDEO_CONTENT_TOP),
        )
        UnifiedOnboardingBody()
        Spacer(modifier = Modifier.height(trailingSpace + 48.dp))
    }
}

@Composable
private fun UnifiedOnboardingBody() {
    OnboardingHeadline(
        title = resourceReference(R.string.tangempay_title),
        subtitle = resourceReference(R.string.tangempay_unified_onboarding_description),
    )
    Spacer(modifier = Modifier.height(24.dp))
    UnifiedOnboardingSections()
}

@Composable
private fun UnifiedOnboardingSections() {
    val primaryColor = TangemTheme.colors3.text.primary
    val secondaryColor = TangemTheme.colors3.text.secondary

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Section(header = stringResourceSafe(R.string.tangempay_unified_onboarding_payment_account_header)) {
            FeatureCardsRow(
                start = {
                    FeatureCard(
                        modifier = it,
                        caption = stringResourceSafe(R.string.tangempay_unified_onboarding_cashback_caption),
                        text = formattedText(
                            formatResId = R.string.tangempay_unified_onboarding_cashback_title,
                            arg = stringResourceSafe(R.string.tangempay_unified_onboarding_cashback_subtitle),
                            argColor = secondaryColor,
                            textColor = primaryColor,
                        ),
                    )
                },
                end = {
                    FeatureCard(
                        modifier = it,
                        caption = stringResourceSafe(R.string.tangempay_unified_onboarding_payment_account_caption),
                        text = formattedText(
                            formatResId = R.string.tangempay_unified_onboarding_payment_account_title,
                            arg = stringResourceSafe(
                                R.string.tangempay_unified_onboarding_payment_account_subtitle_android,
                            ),
                            argColor = primaryColor,
                            textColor = primaryColor,
                        ),
                    )
                },
            )
        }
        Section(header = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_header)) {
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                caption = stringResourceSafe(R.string.tangempay_unified_onboarding_fees_caption),
                text = formattedText(
                    formatResId = R.string.tangempay_unified_onboarding_fees_title,
                    arg = stringResourceSafe(R.string.tangempay_unified_onboarding_fees_subtitle),
                    argColor = primaryColor,
                    textColor = secondaryColor,
                ),
            )
            FeatureCardsRow(
                start = {
                    FeatureCard(
                        modifier = it,
                        caption = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_caption),
                        text = formattedText(
                            formatResId = R.string.tangempay_unified_onboarding_money_account_title,
                            arg = stringResourceSafe(R.string.tangempay_unified_onboarding_money_account_subtitle),
                            argColor = primaryColor,
                            textColor = primaryColor,
                        ),
                    )
                },
                end = {
                    FeatureCard(
                        modifier = it,
                        caption = stringResourceSafe(R.string.tangempay_unified_onboarding_payments_caption),
                        text = AnnotatedString(
                            stringResourceSafe(R.string.tangempay_unified_onboarding_payments_title),
                        ),
                    )
                },
            )
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                caption = stringResourceSafe(R.string.tangempay_unified_onboarding_interest_caption),
                text = AnnotatedString(stringResourceSafe(R.string.tangempay_unified_onboarding_interest_title)),
            )
        }
    }
}

@Composable
private fun FeatureCardsRow(
    start: @Composable (Modifier) -> Unit,
    end: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val cardModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        start(cardModifier)
        end(cardModifier)
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
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun formattedText(
    @StringRes formatResId: Int,
    arg: String,
    argColor: Color,
    textColor: Color,
): AnnotatedString {
    val text = stringResourceSafe(formatResId, arg)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) { append(text) }
        val start = text.indexOf(arg)
        if (start >= 0) addStyle(SpanStyle(color = argColor), start, start + arg.length)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayUnifiedOnboardingLayoutPreview() {
    TangemThemePreviewRedesign {
        TangemPayUnifiedOnboardingLayout(
            video = null,
            isGetStartedLoading = false,
            onGetStartedClick = {},
            onTermsClick = {},
            onCloseClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}