package com.tangem.feature.swap.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImage
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.shareText
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
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
                onExploreClick = state.onSecondaryButtonClick,
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
        TransactionDoneTitle(titleRes = R.string.swapping_success_view_title, date = 0L)
        SpacerH16()
        InputRowImage(
            title = TextReference.Res(R.string.swapping_success_from_title),
            subtitle = state.fromTokenAmount,
            caption = state.fromTokenFiatAmount,
            tokenIconState = state.fromTokenIconState ?: TokenIconState.Loading,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
        SpacerH16()
        InputRowImage(
            title = TextReference.Res(R.string.swapping_success_to_title),
            subtitle = state.toTokenAmount,
            caption = state.toTokenFiatAmount,
            tokenIconState = state.toTokenIconState ?: TokenIconState.Loading,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
        SpacerH16()
        InputRowBestRate(
            imageUrl = state.selectedProvider.imageLarge,
            title = TextReference.Str(state.selectedProvider.name),
            titleExtra = TextReference.Str(state.selectedProvider.type.name),
            subtitle = state.rate,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
        SpacerH16()
        InputRowDefault(
            title = TextReference.Res(R.string.common_fee_label),
            text = state.fee,
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
    }
}

@Composable
private fun SwapSuccessScreenButtons(
    @StringRes textRes: Int,
    txUrl: String,
    onExploreClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .padding(TangemTheme.dimens.spacing16),
    ) {
        if (txUrl.isNotBlank()) {
            Row {
                SecondaryButtonIconStart(
                    text = stringResource(id = com.tangem.core.ui.R.string.common_explore),
                    iconResId = com.tangem.core.ui.R.drawable.ic_web_24,
                    onClick = onExploreClick,
                    modifier = Modifier.weight(1f),
                )
                SpacerW12()
                SecondaryButtonIconStart(
                    text = stringResource(id = com.tangem.core.ui.R.string.common_share),
                    iconResId = com.tangem.core.ui.R.drawable.ic_share_24,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.shareText(txUrl)
                    },
                    modifier = Modifier.weight(1f),
                )
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
    selectedProvider = SwapProvider(
        providerId = "1inch",
        rateTypes = listOf(),
        name = "1inch",
        type = ExchangeProviderType.DEX,
        imageLarge = "",
    ),
    fromTokenAmount = TextReference.Str("1 000 DAI"),
    toTokenAmount = TextReference.Str("1 000 MATIC"),
    fromTokenFiatAmount = TextReference.Str("1 000 $"),
    toTokenFiatAmount = TextReference.Str("1 000 $"),
    fromTokenIconState = TokenIconState.Loading,
    toTokenIconState = TokenIconState.Loading,
    rate = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
    onSecondaryButtonClick = {},
)

@Preview(showBackground = true)
@Composable
private fun Preview_Success_InLightTheme() {
    TangemTheme(isDark = false) {
        SwapSuccessScreen(state) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Success_InDarkTheme() {
    TangemTheme(isDark = true) {
        SwapSuccessScreen(state) {}
    }
}

// endregion preview
