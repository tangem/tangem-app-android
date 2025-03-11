package com.tangem.feature.referral.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.referral.presentation.R

@Composable
internal fun AgreementText(@StringRes firstPartResId: Int, onClick: () -> Unit) {
    val agreementText = annotatedAgreementString(firstPart = stringResourceSafe(firstPartResId))
    ClickableText(
        text = agreementText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing54),
        style = TangemTheme.typography.caption2.copy(textAlign = TextAlign.Center),
        maxLines = 2,
        onClick = {
            val clickableSpanStyle = requireNotNull(agreementText.spanStyles.getOrNull(1))
            if (it in clickableSpanStyle.start..clickableSpanStyle.end) {
                onClick()
            }
        },
    )
}

@Composable
private fun annotatedAgreementString(firstPart: String): AnnotatedString {
    return buildAnnotatedString {
        withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
            append("$firstPart ")
        }
        withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
            append(stringResourceSafe(id = R.string.common_terms_and_conditions))
        }
        withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
            append(" ${stringResourceSafe(id = R.string.referral_tos_suffix)}")
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AgreementText() {
    TangemThemePreview {
        Box(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            AgreementText(firstPartResId = R.string.referral_tos_not_enroled_prefix, onClick = {})
        }
    }
}