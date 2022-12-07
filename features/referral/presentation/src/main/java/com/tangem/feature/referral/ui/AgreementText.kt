package com.tangem.feature.referral.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.textColor
import com.tangem.feature.referral.presentation.R

@Composable
internal fun AgreementText(@StringRes firstPartResId: Int, onClicked: () -> Unit) {
    val agreementText = annotatedAgreementString(firstPart = stringResource(firstPartResId))
    ClickableText(
        text = agreementText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing54),
        style = MaterialTheme.typography.caption.copy(textAlign = TextAlign.Center),
        maxLines = 2,
        onClick = {
            val clickableSpanStyle = requireNotNull(agreementText.spanStyles.getOrNull(1))
            if (it in clickableSpanStyle.start..clickableSpanStyle.end) {
                onClicked()
            }
        },
    )
}

@Composable
private fun annotatedAgreementString(firstPart: String): AnnotatedString {
    return buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY))) {
            append("$firstPart ")
        }
        withStyle(SpanStyle(color = MaterialTheme.colors.textColor(type = TextColorType.ACCENT))) {
            append(stringResource(id = R.string.common_terms_and_conditions))
        }
        withStyle(SpanStyle(color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY))) {
            append(" ${stringResource(id = R.string.referral_tos_suffix)}")
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_AgreementText_InLightTheme() {
    TangemTheme(isDark = false) {
        Box(modifier = Modifier.background(MaterialTheme.colors.primary)) {
            AgreementText(firstPartResId = R.string.referral_tos_not_enroled_prefix, onClicked = {})
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_AgreementText_InDarkTheme() {
    TangemTheme(isDark = true) {
        Box(modifier = Modifier.background(MaterialTheme.colors.primary)) {
            AgreementText(firstPartResId = R.string.referral_tos_not_enroled_prefix, onClicked = {})
        }
    }
}
