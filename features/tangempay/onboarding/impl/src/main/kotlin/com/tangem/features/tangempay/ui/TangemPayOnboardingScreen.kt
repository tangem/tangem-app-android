package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_32
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.core.ui.test.TangemPayTestTags

@Composable
internal fun TangemPayOnboardingScreen(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayOnboardingScreenState.Loading -> TangemPayOnboardingLoading(
            onBack = state.onBack,
            modifier = modifier,
        )
        is TangemPayOnboardingScreenState.Content -> if (state.isUnifiedOnboarding) {
            TangemPayUnifiedOnboardingLayout(
                modifier = modifier,
                isGetStartedLoading = state.buttonConfig.isLoading,
                onGetStartedClick = state.buttonConfig.onClick,
                onTermsClick = state.onTermsClick,
                onTermsAndFeesClick = state.onTermsAndFeesClick,
                onCloseClick = state.onBack,
            )
        } else {
            TangemPayOnboardingLayout(
                modifier = modifier,
                isGetCardLoading = state.buttonConfig.isLoading,
                onGetCardClick = state.buttonConfig.onClick,
                onTermsClick = state.onTermsClick,
                getCardIcon = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
                onCloseClick = state.onBack,
            )
        }
        is TangemPayOnboardingScreenState.NotAvailable ->
            TangemPayOnboardingUnavailable(onBack = state.onBack, modifier = modifier)
    }
}

@Composable
private fun TangemPayOnboardingLoading(onBack: () -> Unit, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(endButton = { TangemButton.Close(onClick = onBack) })
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            TangemLoader(
                color = TangemTheme.colors3.icon.primary,
                size = TangemLoaderSize.X32,
            )
        }
    }
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
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(TangemPayTestTags.ONBOARDING_UNAVAILABLE_ICON),
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayOnboardingScreenPreview(
    @PreviewParameter(TangemPayOnboardingScreenStateProvider::class)
    state: TangemPayOnboardingScreenState,
) {
    TangemThemePreviewRedesign {
        TangemPayOnboardingScreen(state = state, modifier = Modifier.fillMaxSize())
    }
}

private class TangemPayOnboardingScreenStateProvider :
    CollectionPreviewParameterProvider<TangemPayOnboardingScreenState>(
        listOf(
            TangemPayOnboardingScreenState.Loading(onBack = {}),
            TangemPayOnboardingScreenState.Content(
                onBack = {},
                onTermsClick = {},
                onTermsAndFeesClick = {},
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(isLoading = false, onClick = {}),
                isUnifiedOnboarding = false,
            ),
            TangemPayOnboardingScreenState.Content(
                onBack = {},
                onTermsClick = {},
                onTermsAndFeesClick = {},
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(isLoading = true, onClick = {}),
                isUnifiedOnboarding = false,
            ),
            TangemPayOnboardingScreenState.Content(
                onBack = {},
                onTermsClick = {},
                onTermsAndFeesClick = {},
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(isLoading = false, onClick = {}),
                isUnifiedOnboarding = true,
            ),
            TangemPayOnboardingScreenState.NotAvailable(onBack = {}),
        ),
    )