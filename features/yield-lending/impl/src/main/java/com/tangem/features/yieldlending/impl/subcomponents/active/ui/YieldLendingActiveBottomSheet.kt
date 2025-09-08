package com.tangem.features.yieldlending.impl.subcomponents.active.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yieldlending.impl.R
import com.tangem.features.yieldlending.impl.subcomponents.active.entity.YieldLendingActiveContentUM
import com.tangem.features.yieldlending.impl.subcomponents.active.entity.YieldLendingActiveUM

@Composable
internal fun YieldLendingActiveBottomSheet(
    config: TangemBottomSheetConfig,
    onCloseClick: () -> Unit,
    onClick: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<YieldLendingActiveContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { state ->
            AnimatedContent(state) { currentState ->
                when (currentState) {
                    is YieldLendingActiveContentUM.Main -> YieldLendingActiveTitle(onCloseClick = onCloseClick)
                    YieldLendingActiveContentUM.StopEarning ->
                        TangemModalBottomSheetTitle(
                            startIconRes = R.drawable.ic_back_24,
                        )
                }
            }

        },
        footer = { state ->
            AnimatedContent(
                state
            ) { currentState ->
                when (currentState) {
                    is YieldLendingActiveContentUM.Main -> SecondaryButton(
                        text = stringResourceSafe(R.string.yield_module_earn_sheet_stop_earning_button_title),
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    YieldLendingActiveContentUM.StopEarning -> PrimaryButton(
                        text = stringResourceSafe(R.string.common_confirm),
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

        },
        content = { state ->
            when (state) {
                is YieldLendingActiveContentUM.Main -> YieldLendingActiveMainContent(state)
                YieldLendingActiveContentUM.StopEarning -> TODO()
            }
        },
    )
}

@Composable
private fun YieldLendingActiveMainContent(state: YieldLendingActiveContentUM.Main) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(
            vertical = 8.dp,
            horizontal = 16.dp,
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResourceSafe(R.string.yield_module_earn_sheet_total_earnings_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            ResizableText(
                text = state.totalEarnings.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResourceSafe(R.string.yield_module_earn_sheet_my_funds_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerH4()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_tangem_24),
                    contentDescription = null,
                )
                Text(
                    text = "Aave",
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
            SpacerH8()
            Text(
                text = "Your USDT is now deposited in Aave and earning interest. You hold aUSDT token, which represents your balance and grows over time. When you top up, funds go to Aave to earn interest, minus a transaction fee. Read more",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerH8()
            HorizontalDivider(
                thickness = 0.5.dp,
                color = TangemTheme.colors.stroke.primary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Transfers to Aave",
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerWMax()
                Text(
                    text = "Automatic",
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = TangemTheme.colors.stroke.primary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Available",
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerWMax()
                Text(
                    text = state.availableBalance.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun YieldLendingActiveTitle(onCloseClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = stringResourceSafe(R.string.yield_module_earn_sheet_title),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TangemTheme.colors.icon.accent, CircleShape)
                )
                Text(
                    text = stringResourceSafe(R.string.yield_module_earn_sheet_status_active),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier
                )
            }
        }
        TangemIconButton(
            iconRes = R.drawable.ic_close_24,
            onClick = onCloseClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterEnd)
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldLendingActiveBottomSheet_Preview(
    @PreviewParameter(YieldLendingActiveBottomSheetPreviewProvider::class) params: YieldLendingActiveUM,
) {
    TangemThemePreview {
        YieldLendingActiveBottomSheet(
            config = params.bottomSheetConfig,
            onCloseClick = {},
            onClick = {},
        )
    }
}

private class YieldLendingActiveBottomSheetPreviewProvider : PreviewParameterProvider<YieldLendingActiveUM> {
    override val values: Sequence<YieldLendingActiveUM>
        get() = sequenceOf(
            YieldLendingActiveUM(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = YieldLendingActiveContentUM.Main(
                        totalEarnings = stringReference("0.006994219 USDT"),
                        availableBalance = stringReference("3,210.006994 aUSDT"),
                    )
                )
            )
        )
}
// endregion