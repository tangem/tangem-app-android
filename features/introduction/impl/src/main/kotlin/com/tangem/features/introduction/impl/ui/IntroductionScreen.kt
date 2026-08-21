package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.introduction.impl.ui.state.IntroductionUM

private val HorizontalPadding = 16.dp
private val ButtonSpacing = 8.dp
private val CaptionTopPadding = 16.dp
private val BottomPadding = 12.dp

@Composable
internal fun IntroductionScreen(state: IntroductionUM, modifier: Modifier = Modifier) {
    SystemBarsIconsDisposable(darkIcons = false)

    // The design's white primary button, translucent secondary one and 60%-white caption are the dark-theme
    // tokens, so the screen stays dark whatever the app theme is set to. Re-entering the redesign theme is what
    // recomputes the palette from the local below it.
    CompositionLocalProvider(LocalIsInDarkTheme provides true) {
        TangemThemeRedesign {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
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
    val fullText = stringResourceSafe(id = R.string.introduction_legal, termsTitle, privacyTitle)
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    // Locate each title in the resolved string and splice the links in appearance order: a translation may
    // reorder the placeholders. A title the translation does not contain verbatim is skipped, so the caption
    // degrades to plain text instead of crashing on an invalid substring range.
    val links = listOf(
        Triple(fullText.indexOf(termsTitle), termsTitle, onTermsOfServiceClick),
        Triple(fullText.indexOf(privacyTitle), privacyTitle, onPrivacyPolicyClick),
    )
        .filter { it.first >= 0 }
        .sortedBy { it.first }

    val text = buildAnnotatedString {
        var cursor = 0
        links.forEach { (index, title, onClick) ->
            if (index < cursor) return@forEach
            append(fullText.substring(cursor, index))
            withLink(LinkAnnotation.Clickable(tag = title, linkInteractionListener = { onClick() })) {
                withStyle(linkStyle) { append(title) }
            }
            cursor = index + title.length
        }
        append(fullText.substring(cursor))
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
            state = IntroductionUM(
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onTermsOfServiceClick = {},
                onPrivacyPolicyClick = {},
            ),
        )
    }
}
// endregion Preview