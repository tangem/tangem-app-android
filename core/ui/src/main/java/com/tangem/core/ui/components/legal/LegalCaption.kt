package com.tangem.core.ui.components.legal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * "By continuing, you agree with Terms of service and Privacy Policy" with both document names rendered as
 * clickable links. Without [onPrivacyPolicyClick] the caption is "By continuing, you agree with Terms of service".
 *
 * The link ranges are derived from the localized sentence, so a translation may reorder or omit a document
 * name without breaking the caption.
 */
@Composable
fun LegalCaption(
    onTermsOfServiceClick: () -> Unit,
    onPrivacyPolicyClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val termsTitle = stringResourceSafe(id = R.string.introduction_terms_of_service)
    val privacyTitle = onPrivacyPolicyClick?.let { stringResourceSafe(id = R.string.common_privacy_policy) }
    val caption = if (privacyTitle == null) {
        stringResourceSafe(id = R.string.introduction_terms_of_service_consent, termsTitle)
    } else {
        stringResourceSafe(id = R.string.introduction_legal, termsTitle, privacyTitle)
    }
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    val parts = remember(caption, termsTitle, privacyTitle) {
        splitLegalCaption(
            caption = caption,
            titles = buildMap {
                put(LegalDocument.TermsOfService, termsTitle)
                if (privacyTitle != null) put(LegalDocument.PrivacyPolicy, privacyTitle)
            },
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
                    withLink(LinkAnnotation.Clickable(part.text, linkInteractionListener = { onClick?.invoke() })) {
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

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LegalCaptionPreview() {
    TangemThemePreviewRedesign {
        LegalCaption(onTermsOfServiceClick = {}, onPrivacyPolicyClick = {})
    }
}