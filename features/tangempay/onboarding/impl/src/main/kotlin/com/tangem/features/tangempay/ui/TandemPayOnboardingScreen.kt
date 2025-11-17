package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero

@Composable
internal fun TandemPayOnboardingScreen(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = state.onBack,
                iconRes = R.drawable.ic_back_24,
            )
        },
        contentWindowInsets = WindowInsetsZero,
        content = { paddingValues ->
            val contentModifier = Modifier
                .systemBarsPadding()
                .padding(paddingValues)
                .fillMaxSize()
            when (state) {
                is TangemPayOnboardingScreenState.Loading -> TangemPayOnboardingLoading(modifier = contentModifier)
                is TangemPayOnboardingScreenState.Content -> TangemPayOnboardingContent(
                    state = state,
                    modifier = contentModifier,
                )
            }
        },
    )
}

@Composable
private fun TangemPayOnboardingLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier,
            color = TangemTheme.colors.icon.primary1,
        )
    }
}

@Composable
private fun TangemPayOnboardingContent(state: TangemPayOnboardingScreenState.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_tangem_pay_visa),
                contentDescription = null,
                modifier = Modifier.size(width = 200.dp, height = 130.dp),
            )

            Text(
                text = stringResourceSafe(R.string.tangempay_onboarding_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            )

            TangemPayOnboardingBlocks(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 12.dp),
            )
        }
        FooterButtons(
            modifier = Modifier.padding(bottom = 16.dp),
            primaryButtonConfig = state.buttonConfig,
            onTermsClick = state.onTermsClick,
        )
    }
}

@Composable
internal fun TangemPayOnboardingBlocks(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_security_check_22,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_security_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_security_description),
        )

        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_shopping_basket_22,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_purchases_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_purchases_description),
        )

        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_credit_card_add_22,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_pay_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_pay_description),
        )
    }
}

@Composable
private fun TangemPayOnboardingBlock(
    @DrawableRes painterRes: Int,
    titleRef: TextReference,
    descriptionRef: TextReference,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(id = painterRes),
            contentDescription = null,
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
            tint = TangemTheme.colors.icon.accent,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = titleRef.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = descriptionRef.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun FooterButtons(
    primaryButtonConfig: TangemPayOnboardingScreenState.Content.ButtonConfig,
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.tangem_pay_terms_fees_limits),
            onClick = onTermsClick,
        )
        NavigationPrimaryButton(
            primaryButton = NavigationButton(
                textReference = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                iconRes = R.drawable.ic_tangem_24,
                isIconVisible = true,
                shouldShowProgress = primaryButtonConfig.isLoading,
                onClick = primaryButtonConfig.onClick,
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TandemPayOnboardingScreenPreview(
    @PreviewParameter(TangemPayOnboardingScreenStateProvider::class)
    state: TangemPayOnboardingScreenState,
) {
    TangemThemePreview {
        TandemPayOnboardingScreen(state = state, modifier = Modifier.fillMaxSize())
    }
}

private class TangemPayOnboardingScreenStateProvider :
    CollectionPreviewParameterProvider<TangemPayOnboardingScreenState>(
        listOf(
            TangemPayOnboardingScreenState.Loading(onBack = {}),
            TangemPayOnboardingScreenState.Content(
                onBack = {},
                onTermsClick = {},
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(isLoading = false, onClick = {}),
            ),
            TangemPayOnboardingScreenState.Content(
                onBack = {},
                onTermsClick = {},
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(isLoading = true, onClick = {}),
            ),
        ),
    )