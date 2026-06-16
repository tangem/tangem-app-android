package com.tangem.features.tangempay.hotwallet

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.tangempay.ui.TangemPayOnboardingBlock

@Composable
internal fun TangemPayHotWalletOnboardingScreen(state: TangemPayHotWalletOnboardingUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsetsZero,
        content = { paddingValues ->
            Content(
                state = state,
                modifier = Modifier.padding(paddingValues),
            )
        },
    )
}

@Composable
private fun Content(state: TangemPayHotWalletOnboardingUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TangemTheme.colors.background.primary,
                        Color.Black,
                    ),
                ),
            )
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            text = stringResourceSafe(R.string.tangempay_onboarding_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.img_hot_wallet_onboarding),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
        Features(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
        )
        SpacerH16()
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NavigationPrimaryButton(
                primaryButton = NavigationButton(
                    textReference = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                    iconRes = R.drawable.ic_tangem_24,
                    isIconVisible = true,
                    shouldShowProgress = state.isLoading,
                    onClick = state.onGetCardClick,
                ),
            )
            TosText(onClick = state.onTermsClick)
        }
    }
}

@Composable
private fun TosText(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val termsTemplate = stringResourceSafe(R.string.onboarding_create_wallet_term_of_conditions_text)
    val termsLinkText = stringResourceSafe(R.string.disclaimer_title)
    val termsLinkColor = TangemTheme.colors.text.accent
    Text(
        modifier = modifier.fillMaxWidth(),
        text = buildAnnotatedString {
            appendWithStyledPlaceholder(template = termsTemplate) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "tos_link",
                        styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.None)),
                    ) { onClick() },
                ) {
                    appendColored(text = termsLinkText, color = termsLinkColor)
                }
            }
        },
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Features(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_mobile_wallet_icon_24,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_setup_wallet_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_setup_wallet_description),
        )
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_shopping_basket_24,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_purchases_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_purchases_description),
        )
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_credit_card_add_24,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_pay_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_pay_description),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        TangemPayHotWalletOnboardingScreen(
            state = TangemPayHotWalletOnboardingUM(
                isLoading = false,
                onGetCardClick = {},
                onTermsClick = {},
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}