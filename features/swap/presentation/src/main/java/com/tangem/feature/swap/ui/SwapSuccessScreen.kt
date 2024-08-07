package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImage
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.presentation.R

@Composable
fun SwapSuccessScreen(state: SwapSuccessStateHolder, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        content = { padding ->
            SwapSuccessScreenContent(padding = padding, state = state)
        },
        topBar = {
            AppBarWithBackButton(
                onBackClick = onBack,
                iconRes = R.drawable.ic_close_24,
            )
        },
        bottomBar = {
            SwapSuccessScreenButtons(
                textRes = R.string.common_close,
                txUrl = state.txUrl,
                showStatusButton = state.showStatusButton,
                onExploreClick = state.onExploreButtonClick,
                onStatusClick = state.onStatusButtonClick,
                onDoneClick = onBack,
            )
        },
    )
}

@Composable
private fun SwapSuccessScreenContent(state: SwapSuccessStateHolder, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        TransactionDoneTitle(titleRes = R.string.swapping_success_view_title, date = state.timestamp)
        SpacerH16()
        InputRowImage(
            title = TextReference.Res(R.string.swapping_from_title),
            subtitle = state.fromTokenAmount,
            caption = state.fromTokenFiatAmount,
            tokenIconState = state.fromTokenIconState ?: CurrencyIconState.Loading,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
            showNetworkIcon = true,
        )
        SpacerH16()
        InputRowImage(
            title = TextReference.Res(R.string.swapping_to_title),
            subtitle = state.toTokenAmount,
            caption = state.toTokenFiatAmount,
            tokenIconState = state.toTokenIconState ?: CurrencyIconState.Loading,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
            showNetworkIcon = true,
        )
        SpacerH16()
        InputRowBestRate(
            imageUrl = state.providerIcon,
            title = state.providerName,
            titleExtra = state.providerType,
            subtitle = state.rate,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
        SpacerH16()
        InputRowDefault(
            title = TextReference.Res(R.string.common_network_fee_title),
            text = state.fee,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SwapSuccessScreenButtons(
    @StringRes textRes: Int,
    txUrl: String,
    showStatusButton: Boolean,
    onExploreClick: () -> Unit,
    onStatusClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .padding(TangemTheme.dimens.spacing16),
    ) {
        if (txUrl.isNotBlank()) {
            Row {
                SecondaryButtonIconStart(
                    text = stringResource(id = R.string.common_explore),
                    iconResId = R.drawable.ic_web_24,
                    onClick = onExploreClick,
                    modifier = Modifier.weight(1f),
                )
                if (showStatusButton) {
                    SpacerW12()
                    SecondaryButtonIconStart(
                        text = stringResource(id = R.string.express_cex_status_button_title),
                        iconResId = R.drawable.ic_arrow_top_right_24,
                        onClick = onStatusClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SpacerH12()
        }
        PrimaryButton(
            text = stringResource(id = textRes),
            enabled = true,
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// region preview

private val state = SwapSuccessStateHolder(
    timestamp = 0L,
    txUrl = "https://www.google.com/#q=nam",
    fee = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
    providerName = TextReference.Str("1inch"),
    providerType = TextReference.Str(ExchangeProviderType.DEX.providerName),
    showStatusButton = false,
    providerIcon = "",
    fromTokenAmount = TextReference.Str("1 000 DAI"),
    toTokenAmount = TextReference.Str("1 000 MATIC"),
    fromTokenFiatAmount = TextReference.Str("1 000 $"),
    toTokenFiatAmount = TextReference.Str("1 000 $"),
    fromTokenIconState = CurrencyIconState.Loading,
    toTokenIconState = CurrencyIconState.Loading,
    rate = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
    onExploreButtonClick = {},
    onStatusButtonClick = {},
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Success() {
    TangemThemePreview {
        SwapSuccessScreen(state) {}
    }
}
// endregion preview