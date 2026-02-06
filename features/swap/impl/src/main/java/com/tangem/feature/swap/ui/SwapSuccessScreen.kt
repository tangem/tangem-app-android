package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountTitle
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.preview.SwapSuccessStatePreview
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.common.ui.FeeBlockSuccess

@Composable
fun SwapSuccessScreen(state: SwapSuccessStateHolder, feeSelectorUM: FeeSelectorUM?, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        containerColor = TangemTheme.colors.background.secondary,
        content = { padding ->
            SwapSuccessScreenContent(padding = padding, feeSelectorUM = feeSelectorUM, state = state)
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
                shouldShowStatusButton = state.shouldShowStatusButton,
                onExploreClick = state.onExploreButtonClick,
                onStatusClick = state.onStatusButtonClick,
                onDoneClick = onBack,
            )
        },
    )
}

@Composable
private fun SwapSuccessScreenContent(
    state: SwapSuccessStateHolder,
    feeSelectorUM: FeeSelectorUM?,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        TransactionDoneTitle(
            title = resourceReference(R.string.common_in_progress),
            subtitle = resourceReference(
                R.string.send_date_format,
                wrappedList(
                    state.timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
                    state.timestamp.toTimeFormat(),
                ),
            ),
        )
        SpacerH16()
        SwapAmountBlock(
            title = state.fromTitle,
            subtitle = state.fromTokenAmount,
            caption = state.fromTokenFiatAmount,
            tokenIconState = state.fromTokenIconState,
        )
        SpacerH16()
        SwapAmountBlock(
            title = state.toTitle,
            subtitle = state.toTokenAmount,
            caption = state.toTokenFiatAmount,
            tokenIconState = state.toTokenIconState,
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

        if (feeSelectorUM != null) {
            FeeBlockSuccess(feeSelectorUM)
        } else if (state.fee != null && state.fee != TextReference.EMPTY) {
            InputRowDefault(
                title = TextReference.Res(R.string.common_network_fee_title),
                text = state.fee,
                modifier = Modifier
                    .clip(TangemTheme.shapes.roundedCornersXMedium)
                    .background(TangemTheme.colors.background.action),
            )
        }
    }
}

@Composable
private fun SwapAmountBlock(
    title: AccountTitleUM,
    subtitle: TextReference,
    caption: TextReference,
    tokenIconState: CurrencyIconState?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        AccountTitle(title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CurrencyIcon(
                state = tokenIconState ?: CurrencyIconState.Loading,
                shouldDisplayNetwork = true,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = caption.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SwapSuccessScreenButtons(
    @StringRes textRes: Int,
    txUrl: String,
    shouldShowStatusButton: Boolean,
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
                    text = stringResourceSafe(id = R.string.common_explore),
                    iconResId = R.drawable.ic_web_24,
                    onClick = onExploreClick,
                    modifier = Modifier.weight(1f),
                )
                if (shouldShowStatusButton) {
                    SpacerW12()
                    SecondaryButtonIconStart(
                        text = stringResourceSafe(id = R.string.express_cex_status_button_title),
                        iconResId = R.drawable.ic_arrow_top_right_24,
                        onClick = onStatusClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SpacerH12()
        }
        PrimaryButton(
            text = stringResourceSafe(id = textRes),
            enabled = true,
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// region preview
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Success() {
    TangemThemePreview {
        SwapSuccessScreen(SwapSuccessStatePreview.state, null) {}
    }
}
// endregion preview