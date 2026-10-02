package com.tangem.features.moneyaccount.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.moneyaccount.onboarding.impl.R
import com.tangem.core.ui.R as CoreUiR

private const val HEADER_IMAGE_ASPECT_RATIO = 402f / 568f
private const val GRADIENT_TRANSPARENT_STOP = 0.49f
private const val GRADIENT_HALF_STOP = 0.71f
private const val GRADIENT_OPAQUE_STOP = 0.925f
private const val GRADIENT_HALF_ALPHA = 0.5f

@Composable
internal fun MoneyAccountOnboardingScreen(state: MoneyAccountOnboardingUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        when (state) {
            is MoneyAccountOnboardingUM.Loading -> Loading(modifier = Modifier.fillMaxSize())
            is MoneyAccountOnboardingUM.Content -> Content(state = state)
        }

        TangemButton.Close(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 16.dp),
            onClick = state.onBack,
        )
    }

    if (state is MoneyAccountOnboardingUM.Content) {
        MoneyAccountFeesBottomSheet(
            fees = state.fees,
            onDismiss = state.onFeesDismiss,
        )
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors3.icon.primary)
    }
}

@Composable
private fun Content(state: MoneyAccountOnboardingUM.Content, modifier: Modifier = Modifier) {
    var footerHeight by remember { mutableIntStateOf(0) }
    val footerHeightDp = with(LocalDensity.current) { footerHeight.toDp() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = footerHeightDp),
        ) {
            Header()
            Cards(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 12.dp,
                ),
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 24.dp,
                        bottom = 12.dp,
                    ),
                text = stringResourceSafe(CoreUiR.string.money_account_onboarding_disclaimer),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }

        TangemFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> footerHeight = size.height },
            extraTop = { TermsText(onTermsClick = state.onTermsClick) },
        ) {
            TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(CoreUiR.string.money_account_onboarding_secondary_button),
                    onClick = state.onTermsAndFeesClick,
                )
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(CoreUiR.string.common_get_started),
                    isLoading = state.isLoading,
                    onClick = state.onGetStartedClick,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    val background = TangemTheme.colors3.bg.primary
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .aspectRatio(HEADER_IMAGE_ASPECT_RATIO),
            painter = painterResource(id = R.drawable.bg_money_account_onboarding),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to background.copy(alpha = 0f),
                            GRADIENT_TRANSPARENT_STOP to background.copy(alpha = 0f),
                            GRADIENT_HALF_STOP to background.copy(alpha = GRADIENT_HALF_ALPHA),
                            GRADIENT_OPAQUE_STOP to background,
                            1f to background,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResourceSafe(CoreUiR.string.money_account_onboarding_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(CoreUiR.string.money_account_onboarding_description),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun Cards(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            verticalSpacing = 4.dp,
        ) {
            CardCaption(text = stringResourceSafe(CoreUiR.string.money_account_onboarding_transfers_card_caption))
            Text(
                text = buildAnnotatedString {
                    append(stringResourceSafe(CoreUiR.string.money_account_onboarding_transfers_card_title))
                    append(" ")
                    appendColored(
                        text = stringResourceSafe(CoreUiR.string.money_account_onboarding_transfers_card_subtitle),
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountCurrencyCard(
                modifier = Modifier.weight(1f),
            )
            EarnCard(
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Card(verticalSpacing: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
private fun CardCaption(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun CardValue(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
    )
}

@Composable
private fun EarnCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        verticalSpacing = 8.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardCaption(text = stringResourceSafe(CoreUiR.string.money_account_onboarding_earn_card_caption))
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.ic_info_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        }
        // TODO v_rodionov: #[REDACTED_TASK_KEY] get this info from grow service
        CardValue(text = "4,56% APY")
    }
}

@Composable
private fun AccountCurrencyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        verticalSpacing = 8.dp,
    ) {
        CardCaption(text = stringResourceSafe(CoreUiR.string.money_account_onboarding_currency_card_caption))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.img_flag_usa_44),
                contentDescription = null,
            )
            CardValue(text = "USD")
        }
    }
}

@Composable
private fun TermsText(onTermsClick: () -> Unit) {
    val linkColor = TangemTheme.colors3.text.primary
    val linkText = stringResourceSafe(CoreUiR.string.money_account_onboarding_terms_link)
    val fullText = stringResourceSafe(CoreUiR.string.money_account_onboarding_terms_notice, linkText)
    val linkStart = fullText.indexOf(linkText)
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = buildAnnotatedString {
            append(fullText.substring(0, linkStart))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "MA_TERMS",
                    linkInteractionListener = { onTermsClick() },
                ),
                block = { appendColored(text = linkText, color = linkColor) },
            )
            append(fullText.substring(linkStart + linkText.length))
        },
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Preview(showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountOnboardingScreenPreview(
    @PreviewParameter(MoneyAccountOnboardingStateProvider::class)
    state: MoneyAccountOnboardingUM,
) {
    TangemThemePreviewRedesign {
        MoneyAccountOnboardingScreen(state = state, modifier = Modifier.fillMaxSize())
    }
}

private class MoneyAccountOnboardingStateProvider :
    CollectionPreviewParameterProvider<MoneyAccountOnboardingUM>(
        listOf(
            MoneyAccountOnboardingUM.Loading(onBack = {}),
            previewContent(isLoading = false),
            previewContent(isLoading = true),
        ),
    )

private fun previewContent(isLoading: Boolean) = MoneyAccountOnboardingUM.Content(
    onBack = {},
    isLoading = isLoading,
    fees = null,
    onGetStartedClick = {},
    onTermsAndFeesClick = {},
    onTermsClick = {},
    onFeesDismiss = {},
)