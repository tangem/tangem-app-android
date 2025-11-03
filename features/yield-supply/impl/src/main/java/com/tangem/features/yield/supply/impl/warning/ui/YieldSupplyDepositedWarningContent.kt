package com.tangem.features.yield.supply.impl.warning.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyDepositedWarningContent(warningUM: YieldSupplyDepositedWarningUM, onDismiss: () -> Unit) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.primary,
        onBack = null,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = CoreUiR.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            Content(warningUM)
        },
        footer = {
            SecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(CoreUiR.string.balance_hidden_got_it_button),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun Content(warningUM: YieldSupplyDepositedWarningUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.primary)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(height = 56.dp, width = 80.dp),
            contentAlignment = Alignment.Center,
        ) {
            CurrencyIcon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp),
                state = warningUM.iconState,
                shouldDisplayNetwork = true,
                iconSize = 48.dp,
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.CenterEnd)
                    .background(TangemTheme.colors.background.primary, CircleShape),
            )
            Image(
                painter = painterResource(id = CoreUiR.drawable.img_aave_22),
                contentDescription = null,
                modifier = Modifier
                    .padding(3.dp)
                    .align(Alignment.CenterEnd)
                    .size(48.dp),
            )
        }

        SpacerH24()

        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.yield_module_balance_info_sheet_title, warningUM.network),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )

        SpacerH8()

        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.yield_module_balance_info_sheet_subtitle),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )

        SpacerH(8.dp)
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewYieldSupplyDepositedWarningContent(
    @PreviewParameter(YieldSupplyWarningContentProvider::class) warningUM: YieldSupplyDepositedWarningUM,
) {
    TangemThemePreview {
        YieldSupplyDepositedWarningContent(warningUM = warningUM, onDismiss = {})
    }
}

private class YieldSupplyWarningContentProvider : PreviewParameterProvider<YieldSupplyDepositedWarningUM> {
    private val iconState = CurrencyIconState.TokenIcon(
        url = null,
        topBadgeIconResId = null,
        fallbackTint = TangemColorPalette.Black,
        fallbackBackground = TangemColorPalette.Meadow,
        isGrayscale = false,
        showCustomBadge = false,
    )

    override val values: Sequence<YieldSupplyDepositedWarningUM>
        get() = sequenceOf(
            YieldSupplyDepositedWarningUM(
                iconState = iconState,
                onWarningAcknowledged = {},
                network = "Ethereum",
            ),
        )
}