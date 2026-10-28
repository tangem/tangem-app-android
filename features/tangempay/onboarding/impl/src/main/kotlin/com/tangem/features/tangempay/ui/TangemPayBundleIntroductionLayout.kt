package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.core.ui.R
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.tangempay.onboarding.impl.R as OnboardingR

private const val FRAME_WIDTH = 402f
private const val FADE_TOP = 412f
private const val FADE_BOTTOM = 652f
private const val CONTENT_TOP = 498f
private const val FADE_STOPS_COUNT = 16

/** Positions are the design's 402-wide frame, scaled with the screen width. */
@Composable
internal fun TangemPayBundleIntroductionLayout(
    isGetCardLoading: Boolean,
    onGetCardClick: () -> Unit,
    onTermsClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                startButton = {
                    Icon(
                        imageVector = Icons.ic_logo_tangem_24,
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.primary,
                    )
                },
                endButtonsGroup = { TermsButton(onClick = onTermsClick) },
            )
        },
        overlay = {
            TangemFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
                extraTop = {
                    LegalCaption(
                        onTermsOfServiceClick = onTermsOfServiceClick,
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                    )
                },
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                    isLoading = isGetCardLoading,
                    onClick = onGetCardClick,
                )
            }
        },
    ) { contentPadding ->
        Poster()
        BundleContent(
            contentPadding = contentPadding,
            trailingSpace = with(LocalDensity.current) { footerHeight.toDp() },
        )
    }
}

@Composable
private fun Poster() {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(OnboardingR.drawable.ill_tangem_pay_bundle_poster),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        alignment = Alignment.TopCenter,
    )
}

@Composable
private fun BundleContent(contentPadding: PaddingValues, trailingSpace: Dp) {
    val layoutDirection = LocalLayoutDirection.current
    val backgroundColor = TangemTheme.colors3.bg.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            )
            .drawWithCache {
                val scale = size.width / FRAME_WIDTH
                // Past endY the clamp paints the solid part
                val brush = Brush.verticalGradient(
                    colorStops = fadeStops(backgroundColor),
                    startY = FADE_TOP * scale,
                    endY = FADE_BOTTOM * scale,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FRAME_WIDTH / CONTENT_TOP),
        )
        OnboardingHeadline(
            title = resourceReference(R.string.tangempay_newonboard_title),
            subtitle = resourceReference(R.string.tangempay_newonboard_body),
        )
        OnboardingBenefits()
        Spacer(modifier = Modifier.height(trailingSpace))
    }
}

private fun fadeStops(color: Color): Array<Pair<Float, Color>> = Array(FADE_STOPS_COUNT) { index ->
    val fraction = index / (FADE_STOPS_COUNT - 1f)
    fraction to color.copy(alpha = EaseInOut.transform(fraction))
}

@Preview(widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBundleIntroductionLayoutPreview() {
    TangemThemePreviewRedesign {
        TangemPayBundleIntroductionLayout(
            modifier = Modifier.fillMaxSize(),
            isGetCardLoading = false,
            onGetCardClick = {},
            onTermsClick = {},
            onTermsOfServiceClick = {},
            onPrivacyPolicyClick = {},
        )
    }
}