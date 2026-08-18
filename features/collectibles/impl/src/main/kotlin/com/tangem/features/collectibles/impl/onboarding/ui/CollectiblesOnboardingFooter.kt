package com.tangem.features.collectibles.impl.onboarding.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.tangem.common.ui.onboarding.OnboardingFooter
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM

/**
 * Pinned footer of the Onboarding screen: the legal line and the CTA over a blurring scrim.
 *
 * @param state screen state — supplies the legal line's link titles and every click handler.
 * @param contentPadding safe-area padding from the scaffold.
 * @param modifier applied to the footer root; the caller aligns it to the bottom and measures its height.
 */
@Composable
internal fun CollectiblesOnboardingFooter(
    state: CollectiblesOnboardingUM,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    OnboardingFooter(contentPadding = contentPadding, modifier = modifier) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(
                id = R.string.prediction_onboarding_legal,
                formatArgs = wrappedList(
                    termsLink(title = state.collectiblesTitle, onClick = state.onCollectiblesTermsClick),
                    termsLink(title = state.tangemTitle, onClick = state.onTangemTermsClick),
                ),
            ).resolveAnnotatedReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )

        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Primary,
            isLoading = state.isCreatingAccount,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
            text = state.createAccountButtonText,
            contentDescription = if (state.isCreatingAccount) stringResourceSafe(R.string.common_in_progress) else null,
            onClick = state.onCreateAccountClick,
        )
    }
}

@Composable
private fun termsLink(title: TextReference, onClick: () -> Unit): TextReference {
    val text = title.resolveReference()
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    return annotatedReference {
        withLink(LinkAnnotation.Clickable(tag = text, linkInteractionListener = { onClick() })) {
            withStyle(linkStyle) { append(text) }
        }
    }
}