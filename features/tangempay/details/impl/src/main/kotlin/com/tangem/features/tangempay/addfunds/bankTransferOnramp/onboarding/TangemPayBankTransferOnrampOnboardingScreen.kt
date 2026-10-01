package com.tangem.features.tangempay.addfunds.bankTransferOnramp.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_government_28
import com.tangem.features.tangempay.addfunds.LegalLink
import com.tangem.features.tangempay.addfunds.TangemPayLegalLinksText
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFees
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.isEmpty
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf

private const val GLOW_ALPHA = 0.35f
private const val GLOW_RADIUS_FACTOR = 1.1f
private val IllustrationTopPadding = 133.dp
private val IllustrationBottomPadding = 155.dp
private val DisclaimerLineHeight = 18.sp

@Composable
internal fun TangemPayBankTransferOnrampOnboardingScreen(
    state: TangemPayBankTransferOnrampOnboardingScreenUM,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }
    val footerHeightDp = with(LocalDensity.current) { footerHeight.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = footerHeightDp),
        ) {
            Header()
            FeesBlock(fees = state.fees, modifier = Modifier.padding(top = 12.dp))
            Disclaimer()
        }
        TangemFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> footerHeight = size.height },
            extraTop = { TermsText(onTermsClick = state.onTermsClick) },
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_continue),
                isLoading = state.isLoading,
                onClick = state.onContinueClick,
            )
        }
        TangemButton.Close(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp),
            onClick = state.onCloseClick,
        )
    }
}

@Composable
private fun Header() {
    val accentBlue = TangemTheme.colors3.icon.accent.blue
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(accentBlue, Color.Transparent)))
                .statusBarsPadding()
                .padding(top = IllustrationTopPadding, bottom = IllustrationBottomPadding),
            contentAlignment = Alignment.Center,
        ) {
            GovernmentIllustration(color = TangemTheme.colors3.icon.brand)
        }
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.tangempay_bank_transfer_onboarding_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(R.string.tangempay_bank_transfer_onboarding_description),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun GovernmentIllustration(color: Color) {
    Icon(
        modifier = Modifier
            .size(144.dp)
            .drawBehind {
                val radius = size.maxDimension * GLOW_RADIUS_FACTOR
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = GLOW_ALPHA), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
            },
        imageVector = Icons.ic_government_28,
        contentDescription = null,
        tint = color,
    )
}

@Composable
private fun FeesBlock(fees: TangemPayOnrampFeesUM, modifier: Modifier = Modifier) {
    if (fees.isEmpty) return
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .padding(bottom = 12.dp),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            text = stringResourceSafe(R.string.tangempay_bank_transfer_onboarding_fee_header),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemPayOnrampFees(fees = fees, containerColor = Color.Transparent)
    }
}

@Composable
private fun Disclaimer() {
    val swiftWarning = stringResourceSafe(R.string.tangempay_bank_transfer_swift_warning)
    val providerDisclaimer = stringResourceSafe(R.string.money_account_onboarding_disclaimer)
    Text(
        modifier = Modifier.padding(16.dp),
        text = "$swiftWarning. $providerDisclaimer",
        style = TangemTheme.typography3.caption.medium.copy(lineHeight = DisclaimerLineHeight),
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun TermsText(onTermsClick: () -> Unit) {
    val termsTitle = stringResourceSafe(R.string.tangempay_bank_transfer_terms_of_use)
    TangemPayLegalLinksText(
        fullText = stringResourceSafe(R.string.tangempay_bank_transfer_legal, termsTitle),
        links = listOf(LegalLink(termsTitle, onTermsClick)),
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 1131)
@Preview(showBackground = true, widthDp = 402, heightDp = 1131, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankTransferOnrampOnboardingScreenPreview(
    @PreviewParameter(BankTransferOnrampOnboardingPreviewProvider::class)
    state: TangemPayBankTransferOnrampOnboardingScreenUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankTransferOnrampOnboardingScreen(state = state)
    }
}

private class BankTransferOnrampOnboardingPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayBankTransferOnrampOnboardingScreenUM>(
        collection = listOf(
            previewState(fees = previewFees()),
            previewState(fees = TangemPayOnrampFeesUM.Loading),
            previewState(fees = TangemPayOnrampFeesUM.Error(onRetryClick = {})),
            previewState(fees = previewFees(), isLoading = true),
        ),
    )

private fun previewState(fees: TangemPayOnrampFeesUM, isLoading: Boolean = false) =
    TangemPayBankTransferOnrampOnboardingScreenUM(
        fees = fees,
        isLoading = isLoading,
        onContinueClick = {},
        onTermsClick = {},
        onCloseClick = {},
    )

private fun previewFees() = TangemPayOnrampFeesUM.Content(
    rows = persistentListOf(
        TangemPayOnrampFeesUM.FeeRow(type = "FEDWIRE_ONRAMP", title = stringReference("FedWire"), value = "$0"),
        TangemPayOnrampFeesUM.FeeRow(type = "ACH_ONRAMP", title = stringReference("ACH"), value = "$0"),
    ),
)