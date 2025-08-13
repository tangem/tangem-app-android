package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.R

private const val TERMS_OF_USE_KEY = "termsOfUse"
private const val PRIVACY_POLICY_KEY = "privacyPolicy"

private const val TERMS_OF_USE_URL = "https://docs.yield.xyz/docs/terms-of-use#/"
private const val PRIVACY_POLICY_URL = "https://docs.yield.xyz/docs/privacy-policy#/"

@Composable
internal fun StakingTosText(onTextClick: (String) -> Unit) {
    val termsOfUse = stringResourceSafe(R.string.common_terms_of_use)
    val privacyPolicy = stringResourceSafe(R.string.common_privacy_policy)
    val tosText = stringResourceSafe(R.string.staking_legal, termsOfUse, privacyPolicy)

    val clickableAnnotation = buildAnnotatedString {
        append(tosText.substringBefore(termsOfUse))

        pushStringAnnotation(TERMS_OF_USE_KEY, "")
        appendColored(termsOfUse, TangemTheme.colors.text.accent)
        pop()

        append(tosText.substringAfter(termsOfUse).substringBefore(privacyPolicy))

        pushStringAnnotation(PRIVACY_POLICY_KEY, "")
        appendColored(privacyPolicy, TangemTheme.colors.text.accent)
        pop()
    }

    ClickableText(
        text = clickableAnnotation,
        style = TangemTheme.typography.caption2.copy(
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        ),
        onClick = { offset ->
            clickableAnnotation.getStringAnnotations(
                tag = TERMS_OF_USE_KEY,
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
                onTextClick(TERMS_OF_USE_URL)
            }

            clickableAnnotation.getStringAnnotations(
                tag = PRIVACY_POLICY_KEY,
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
                onTextClick(PRIVACY_POLICY_URL)
            }
        },
    )
}