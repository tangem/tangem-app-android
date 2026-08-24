package com.tangem.features.introduction.impl.ui

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.KeepScreenOn
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.core.ui.R as CoreUiR

private val HorizontalPadding = 16.dp
private val LogoTopPadding = 16.dp
private val LogoHeight = 20.dp
private val ButtonSpacing = 8.dp
private val CaptionTopPadding = 16.dp
private val BottomPadding = 12.dp

private val ScrimColor = Color(0xFF0F0F0F)

private val BottomScrimBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.52f to ScrimColor.copy(alpha = 0f),
        0.63f to ScrimColor.copy(alpha = 0.5f),
        0.70f to ScrimColor.copy(alpha = 0.8f),
        0.76f to ScrimColor.copy(alpha = 0.96f),
        0.82f to ScrimColor,
        1f to ScrimColor,
    ),
)

@Composable
internal fun IntroductionScreen(
    state: IntroductionUM,
    onSurfaceAvailable: (SurfaceView) -> Unit,
    onSurfaceRelease: (SurfaceView) -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsIconsDisposable(darkIcons = false)
    // Nothing moves under reduced motion, so there is nothing to keep the display awake for.
    if (state.isMotionEnabled) KeepScreenOn()

    // The design's colours are the dark-theme tokens, and re-entering the theme is what recomputes them.
    CompositionLocalProvider(LocalIsInDarkTheme provides true) {
        TangemThemeRedesign {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                // A SurfaceView punches a hole through the window, erasing whatever is composed before it,
                // so the shutter below has to come after it.
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> SurfaceView(context).also(onSurfaceAvailable) },
                    onRelease = onSurfaceRelease,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (state.isVideoReady) 0f else 1f }
                        .background(Color.Black),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BottomScrimBrush),
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = HorizontalPadding, top = LogoTopPadding)
                        .height(LogoHeight),
                    painter = painterResource(id = CoreUiR.drawable.ic_tangem_logo),
                    contentScale = ContentScale.FillHeight,
                    contentDescription = null,
                )
                BottomActions(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = HorizontalPadding)
                        .padding(bottom = BottomPadding),
                )
            }
        }
    }
}

@Composable
private fun BottomActions(state: IntroductionUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ButtonSpacing),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.introduction_button_create_wallet),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = state.onCreateWalletClick,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.introduction_button_i_have_a_wallet),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            onClick = state.onIHaveWalletClick,
        )
        LegalCaption(
            modifier = Modifier.padding(top = CaptionTopPadding),
            onTermsOfServiceClick = state.onTermsOfServiceClick,
            onPrivacyPolicyClick = state.onPrivacyPolicyClick,
        )
    }
}

@Composable
private fun LegalCaption(
    onTermsOfServiceClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val termsTitle = stringResourceSafe(id = R.string.introduction_terms_of_service)
    val privacyTitle = stringResourceSafe(id = R.string.common_privacy_policy)
    val caption = stringResourceSafe(id = R.string.introduction_legal, termsTitle, privacyTitle)
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    val parts = remember(caption, termsTitle, privacyTitle) {
        splitLegalCaption(
            caption = caption,
            titles = mapOf(
                LegalDocument.TermsOfService to termsTitle,
                LegalDocument.PrivacyPolicy to privacyTitle,
            ),
        )
    }

    val text = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is LegalCaptionPart.Plain -> append(part.text)
                is LegalCaptionPart.Link -> {
                    val onClick = when (part.document) {
                        LegalDocument.TermsOfService -> onTermsOfServiceClick
                        LegalDocument.PrivacyPolicy -> onPrivacyPolicyClick
                    }
                    withLink(LinkAnnotation.Clickable(part.text, linkInteractionListener = { onClick() })) {
                        withStyle(linkStyle) { append(part.text) }
                    }
                }
            }
        }
    }

    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun IntroductionScreenPreview() {
    TangemThemePreview {
        IntroductionScreen(
            onSurfaceAvailable = {},
            onSurfaceRelease = {},
            state = IntroductionUM(
                isVideoReady = true,
                isMotionEnabled = true,
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onTermsOfServiceClick = {},
                onPrivacyPolicyClick = {},
            ),
        )
    }
}
// endregion Preview