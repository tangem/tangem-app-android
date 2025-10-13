package com.tangem.features.onramp.main.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM

private const val TERMS_OF_USE_KEY = "termsOfUse"
private const val PRIVACY_POLICY_KEY = "privacyPolicy"

@Composable
internal fun OnrampButtonComponent(state: OnrampMainComponentUM) {
    val content = state as? OnrampMainComponentUM.Content
    val providerState = content?.providerBlockState as? OnrampProviderBlockUM.Content
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OnrampTosText(providerState)
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(id = R.string.common_buy),
            onClick = state.buyButtonConfig.onClick,
            enabled = state.buyButtonConfig.enabled,
        )
    }
}

@Composable
private fun OnrampTosText(provider: OnrampProviderBlockUM.Content?) {
    val termsOfUse = stringResourceSafe(R.string.common_terms_of_use)
    val privacyPolicy = stringResourceSafe(R.string.common_privacy_policy)
    val tosText = stringResourceSafe(R.string.onramp_legal, termsOfUse, privacyPolicy)

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

    AnimatedContent(
        targetState = provider,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        label = "Onramp Legal Info Animation",
    ) { state ->
        val termsOfUseLink = provider?.termsOfUseLink
        val privacyPolicyLink = provider?.privacyPolicyLink

        if (state != null && termsOfUseLink != null && privacyPolicyLink != null) {
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
                        state.onLinkClick(termsOfUseLink)
                    }

                    clickableAnnotation.getStringAnnotations(
                        tag = PRIVACY_POLICY_KEY,
                        start = offset,
                        end = offset,
                    ).firstOrNull()?.let {
                        state.onLinkClick(privacyPolicyLink)
                    }
                },
            )
        }
    }
}