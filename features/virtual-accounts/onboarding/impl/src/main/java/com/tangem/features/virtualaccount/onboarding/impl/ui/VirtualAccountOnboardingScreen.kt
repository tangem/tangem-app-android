package com.tangem.features.virtualaccount.onboarding.impl.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.appendWithStyledPlaceholders
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewColumn
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.payment.impl.ui.PaymentOnboardingFeatureBlock
import com.tangem.features.virtualaccount.onboarding.impl.ui.state.VirtualAccountOnboardingUiState
import com.tangem.features.virtualaccount.onboarding.impl.R as OnboardingR

@Composable
internal fun VirtualAccountOnboardingScreen(
    state: VirtualAccountOnboardingUiState,
    onBackClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            TangemTopAppBar(
                title = null,
                startButton = TopAppBarButtonUM.Back(onBackClicked = onBackClick),
                endButton = TopAppBarButtonUM.Text(
                    text = resourceReference(OnboardingR.string.common_learn_more),
                    onTextClicked = onLearnMoreClick,
                ),
            )
        },
        contentWindowInsets = WindowInsetsZero,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.size(32.dp))
                PromoContent()
            }
            BottomContent(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                onContinueClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun PromoContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderIconsRow(
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Spacer(modifier = Modifier.size(20.dp))
        TextBlock(
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.size(32.dp))
        FeaturesBlock(
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun HeaderIconsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletIcon(
            backgroundColor = Color.Black,
            iconRes = R.drawable.ic_tangem_24,
        )
        Spacer(modifier = Modifier.size(12.dp))
        IconsDivider()
        Spacer(modifier = Modifier.size(12.dp))
        WalletIcon(
            backgroundColor = Color(BANK_ICON_COLOR),
            iconRes = R.drawable.ic_bank_40,
        )
    }
}

@Composable
private fun RowScope.IconsDivider() {
    Box(
        modifier = Modifier
            .alpha(DIVIDER_ALPHA)
            .size(8.dp)
            .background(
                color = TangemTheme.colors.icon.informative,
                shape = CircleShape,
            ),
    )
    Spacer(modifier = Modifier.size(4.dp))
    Box(
        modifier = Modifier
            .alpha(DIVIDER_ALPHA)
            .width(18.dp)
            .height(1.dp)
            .background(
                color = TangemTheme.colors.icon.informative,
                shape = CircleShape,
            ),
    )
    Spacer(modifier = Modifier.size(4.dp))
    Box(
        modifier = Modifier
            .alpha(DIVIDER_ALPHA)
            .size(8.dp)
            .background(
                color = TangemTheme.colors.icon.informative,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun WalletIcon(backgroundColor: Color, @DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun TextBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = OnboardingR.string.virtual_account_onboarding_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResourceSafe(id = OnboardingR.string.virtual_account_onboarding_subtitle),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeaturesBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PaymentOnboardingFeatureBlock(
            iconPainter = painterResource(R.drawable.ic_flash_new_24),
            title = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_1_title),
            description = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_1_description),
        )
        PaymentOnboardingFeatureBlock(
            iconPainter = painterResource(R.drawable.ic_security_check_24),
            title = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_2_title),
            description = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_2_description),
        )
        PaymentOnboardingFeatureBlock(
            iconPainter = painterResource(R.drawable.ic_repeat_24),
            title = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_3_title),
            description = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_feature_3_description),
        )
    }
}

@Composable
private fun BottomContent(
    state: VirtualAccountOnboardingUiState,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TermsText(
            termsOfUseUrl = state.termsOfUseUrl,
            privacyPolicyLink = state.privacyPolicyLink,
        )
        Spacer(modifier = Modifier.size(12.dp))
        NavigationPrimaryButton(
            primaryButton = NavigationButton(
                textReference = resourceReference(OnboardingR.string.common_continue),
                onClick = onContinueClick,
            ),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResourceSafe(id = OnboardingR.string.virtual_account_onboarding_powered_by),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(6.dp))
    }
}

@Composable
private fun TermsText(termsOfUseUrl: String, privacyPolicyLink: String, modifier: Modifier = Modifier) {
    val accentColor = TangemTheme.colors.text.accent
    val termsOfUse = stringResourceSafe(OnboardingR.string.common_terms_of_use)
    val privacyPolicy = stringResourceSafe(OnboardingR.string.common_privacy_policy)
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            appendWithStyledPlaceholders(
                template = stringResourceSafe(OnboardingR.string.virtual_account_onboarding_terms_template),
                {
                    withLink(
                        LinkAnnotation.Url(
                            url = termsOfUseUrl,
                            styles = TextLinkStyles(style = SpanStyle(color = accentColor)),
                        ),
                    ) {
                        append(termsOfUse)
                    }
                },
                {
                    withLink(
                        LinkAnnotation.Url(
                            url = privacyPolicyLink,
                            styles = TextLinkStyles(style = SpanStyle(color = accentColor)),
                        ),
                    ) {
                        append(privacyPolicy)
                    }
                },
            )
        },
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.caption1,
        textAlign = TextAlign.Center,
    )
}

private const val BANK_ICON_COLOR = 0xFF45BF8E
private const val DIVIDER_ALPHA = 0.5f

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreviewColumn {
    VirtualAccountOnboardingScreen(
        state = VirtualAccountOnboardingUiState(),
        onBackClick = {},
        onLearnMoreClick = {},
        onContinueClick = {},
    )
}