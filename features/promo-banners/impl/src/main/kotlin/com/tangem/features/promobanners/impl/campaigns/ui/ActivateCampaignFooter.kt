package com.tangem.features.promobanners.impl.campaigns.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.campaigns.entity.FooterUM
import com.tangem.features.promobanners.impl.campaigns.entity.TermsUM

@Composable
internal fun ActivateCampaignFooter(footerUM: FooterUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val terms = footerUM.terms

        if (terms != null) {
            Text(
                text = termsAnnotatedString(terms),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            SpacerH12()
        }

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = footerUM.label.resolveReference(),
            onClick = footerUM.onPrimaryButtonClick,
        )
    }
}

@Composable
private fun termsAnnotatedString(terms: TermsUM) = buildAnnotatedString {
    val startText = terms.text.resolveReference()
    val linkText = terms.linkText.resolveReference()

    append(startText)
    append(" ")
    withLink(
        link = LinkAnnotation.Clickable(
            tag = "CAMPAIGN_TERMS",
            linkInteractionListener = { terms.onTermsClick() },
        ),
    ) {
        withStyle(
            SpanStyle(
                color = TangemTheme.colors3.text.primary,
                textDecoration = TextDecoration.None,
            ),
        ) {
            append(linkText)
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ActivateCampaignFooter_WithTerms() {
    TangemThemePreviewRedesign {
        ActivateCampaignFooter(
            footerUM = CampaignPreviewData.footer,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ActivateCampaignFooter_NoTerms() {
    TangemThemePreviewRedesign {
        ActivateCampaignFooter(
            footerUM = CampaignPreviewData.footer.copy(terms = null),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}
// endregion