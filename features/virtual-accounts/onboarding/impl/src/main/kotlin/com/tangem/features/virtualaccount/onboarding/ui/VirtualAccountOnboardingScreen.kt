package com.tangem.features.virtualaccount.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.virtualaccount.onboarding.impl.R

private const val GRADIENT_TRANSPARENT_STOP = 0.45f
private const val GRADIENT_OPAQUE_STOP = 0.72f

private const val TERMS_LINK_TAG = "VA_TERMS"
private const val PRIVACY_LINK_TAG = "VA_PRIVACY"

@Composable
internal fun VirtualAccountOnboardingScreen(state: VirtualAccountOnboardingUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_virtual_account_onboarding),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to TangemTheme.colors3.bg.primary.copy(alpha = 0f),
                            GRADIENT_TRANSPARENT_STOP to TangemTheme.colors3.bg.primary.copy(alpha = 0f),
                            GRADIENT_OPAQUE_STOP to TangemTheme.colors3.bg.primary,
                            1f to TangemTheme.colors3.bg.primary,
                        ),
                    ),
                ),
        )

        when (state) {
            is VirtualAccountOnboardingUM.Loading -> Loading(modifier = Modifier.fillMaxSize())
            is VirtualAccountOnboardingUM.Content -> Content(state = state)
        }

        TangemButton.Close(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 16.dp),
            onClick = state.onBack,
        )
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TangemTheme.colors3.icon.primary)
    }
}

@Composable
private fun Content(state: VirtualAccountOnboardingUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Send USD from your bank. Receive USDC",
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = "A dedicated account with US banking details — no deposit or maintenance fees",
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }

        TermsCard(
            modifier = Modifier.padding(top = 24.dp, start = 8.dp, end = 8.dp),
            state = state,
        )
    }
}

@Composable
private fun TermsCard(state: VirtualAccountOnboardingUM.Content, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors3.bg.opaque.primary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = shape),
    ) {
        Text(
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            text = buildTermsAndPolicy(
                onTermsClick = state.onTermsClick,
                onPrivacyClick = state.onPrivacyClick,
            ).resolveAnnotatedReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = stringReference("Open account"),
            iconEnd = TangemIconUM.Icon(R.drawable.ic_tangem_24),
            isLoading = state.isLoading,
            onClick = state.onGetCardClick,
        )
    }
}

@Composable
private fun buildTermsAndPolicy(onTermsClick: () -> Unit, onPrivacyClick: () -> Unit) = annotatedReference {
    val linkColor = TangemTheme.colors3.text.primary
    append("By using service, you agree with provider ")
    withLink(
        link = LinkAnnotation.Clickable(
            tag = TERMS_LINK_TAG,
            linkInteractionListener = { onTermsClick() },
        ),
        block = { appendColored(text = "Terms of Use", color = linkColor) },
    )
    append(" and ")
    withLink(
        link = LinkAnnotation.Clickable(
            tag = PRIVACY_LINK_TAG,
            linkInteractionListener = { onPrivacyClick() },
        ),
        block = { appendColored(text = "Privacy Policy", color = linkColor) },
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VirtualAccountOnboardingScreenPreview(
    @PreviewParameter(VirtualAccountOnboardingStateProvider::class)
    state: VirtualAccountOnboardingUM,
) {
    TangemThemePreviewRedesign {
        VirtualAccountOnboardingScreen(state = state, modifier = Modifier.fillMaxSize())
    }
}

private class VirtualAccountOnboardingStateProvider :
    CollectionPreviewParameterProvider<VirtualAccountOnboardingUM>(
        listOf(
            VirtualAccountOnboardingUM.Loading(onBack = {}),
            VirtualAccountOnboardingUM.Content(
                onBack = {},
                isLoading = false,
                onGetCardClick = {},
                onTermsClick = {},
                onPrivacyClick = {},
            ),
            VirtualAccountOnboardingUM.Content(
                onBack = {},
                isLoading = true,
                onGetCardClick = {},
                onTermsClick = {},
                onPrivacyClick = {},
            ),
        ),
    )