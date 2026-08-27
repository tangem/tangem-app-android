package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_32
import com.tangem.core.ui.utils.WindowInsetsZero

@Composable
internal fun TandemPayOnboardingScreen(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayOnboardingScreenState.NotAvailable ->
            TangemPayOnboardingUnavailable(onBack = state.onBack, modifier = modifier)
        is TangemPayOnboardingScreenState.Loading,
        is TangemPayOnboardingScreenState.Content,
        -> TangemPayOnboardingScaffold(state = state, modifier = modifier)
    }
}

@Composable
private fun TangemPayOnboardingScaffold(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
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
                is TangemPayOnboardingScreenState.NotAvailable -> Unit
            }
        },
    )
}

@Composable
private fun TangemPayOnboardingUnavailable(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(top = 72.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.ic_heart_broken_32,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResourceSafe(R.string.tangem_pay_unavailable_region_title),
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                    Text(
                        text = stringResourceSafe(R.string.tangem_pay_unavailable_region_description),
                        style = TangemTheme.typography3.subheading.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_got_it),
                onClick = onBack,
            )
        }
    }
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
                modifier = Modifier
                    .size(width = 204.dp, height = 130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                TangemTheme.colors.text.constantWhite.copy(alpha = 0.1F),
                                TangemTheme.colors.text.constantWhite.copy(alpha = 0f),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
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
        TangemPayOnboardingButtons(
            modifier = Modifier.padding(bottom = 16.dp),
            onGetCardClick = state.buttonConfig.onClick,
            isLoading = state.buttonConfig.isLoading,
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
            painterRes = R.drawable.ic_security_check_24,
            titleRef = TextReference.Res(R.string.tangempay_onboarding_security_title),
            descriptionRef = TextReference.Res(R.string.tangempay_onboarding_security_description),
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TandemPayOnboardingScreenPreview(
    @PreviewParameter(TangemPayOnboardingScreenStateProvider::class)
    state: TangemPayOnboardingScreenState,
) {
    TangemThemePreviewRedesign {
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
            TangemPayOnboardingScreenState.NotAvailable(onBack = {}),
        ),
    )