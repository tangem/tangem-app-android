package com.tangem.features.yield.supply.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.utils.StringsSigns

@Composable
internal fun YieldSupplyBlockContent(yieldSupplyUM: YieldSupplyUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = yieldSupplyUM,
        modifier = modifier,
    ) { supplyUM ->
        when (supplyUM) {
            is YieldSupplyUM.Available -> SupplyAvailable(supplyUM)
            YieldSupplyUM.Loading -> SupplyLoading()
            is YieldSupplyUM.Content -> SupplyContent(supplyUM)
            YieldSupplyUM.Processing.Enter -> SupplyProcessing(
                resourceReference(R.string.yield_module_token_details_earn_notification_processing),
            )
            YieldSupplyUM.Processing.Exit -> SupplyProcessing(
                resourceReference(R.string.yield_module_stop_earning),
            )
            YieldSupplyUM.Unavailable -> SupplyUnavailable()
            YieldSupplyUM.Initial -> {}
        }
    }
}

@Composable
private fun SupplyAvailable(supplyUM: YieldSupplyUM.Available) {
    SupplyInfo(
        title = supplyUM.title,
        subtitle = resourceReference(R.string.yield_module_token_details_earn_notification_description),
        iconTint = TangemTheme.colors.icon.accent,
        button = {
            SecondaryButton(
                text = stringResourceSafe(R.string.common_learn_more),
                onClick = supplyUM.onClick,
                size = TangemButtonSize.WideAction,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun SupplyUnavailable() {
    SupplyInfo(
        title = resourceReference(R.string.yield_module_unavailable_title),
        subtitle = resourceReference(R.string.yield_module_unavailable_subtitle),
        iconTint = TangemTheme.colors.icon.inactive,
        button = null,
    )
}

@Composable
private fun SupplyContent(supplyUM: YieldSupplyUM.Content) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = supplyUM.onClick)
            .padding(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.img_aave_22),
            modifier = Modifier.size(36.dp),
            contentDescription = null,
        )
        SpacerW12()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = supplyUM.title.resolveReference(),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = StringsSigns.DOT,
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.tertiary,
                )
                Text(
                    text = supplyUM.rewardsApy.resolveReference(),
                    style = TangemTheme.typography.subtitle1,
                    maxLines = 1,
                    color = TangemTheme.colors.text.accent,
                )
            }
            Text(
                text = supplyUM.subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.primary1,
            )
        }
        SpacerW8()
        AnimatedVisibility(supplyUM.showWarningIcon) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_alert_triangle_20),
                contentDescription = null,
                tint = TangemTheme.colors.icon.attention,
            )
        }
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SupplyProcessing(text: TextReference) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.img_aave_22),
            modifier = Modifier.size(36.dp),
            contentDescription = null,
        )
        SpacerW12()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResourceSafe(
                    R.string.yield_module_token_details_earn_notification_earning_on_your_balance_title,
                ),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.primary1,
            )
        }
        SpacerW8()
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = TangemTheme.colors.icon.accent,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun SupplyLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.inactive,
                modifier = Modifier
                    .background(TangemTheme.colors.icon.inactive.copy(alpha = 0.1f), CircleShape)
                    .padding(6.dp)
                    .size(24.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextShimmer(
                    text = stringResourceSafe(R.string.yield_module_unavailable_title),
                    style = TangemTheme.typography.button,
                )
                TextShimmer(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResourceSafe(R.string.yield_module_unavailable_subtitle),
                    style = TangemTheme.typography.caption2,
                )
                TextShimmer(
                    modifier = Modifier.width(100.dp),
                    text = stringResourceSafe(R.string.yield_module_unavailable_subtitle),
                    style = TangemTheme.typography.caption2,
                )
            }
        }
        SecondaryButton(
            text = stringResourceSafe(R.string.common_learn_more),
            onClick = { },
            showProgress = true,
            enabled = false,
            size = TangemButtonSize.WideAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SupplyInfo(
    title: TextReference,
    subtitle: TextReference,
    iconTint: Color,
    modifier: Modifier = Modifier,
    button: (@Composable () -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .background(iconTint.copy(alpha = 0.1f), CircleShape)
                    .padding(6.dp)
                    .size(24.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        button?.invoke()
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyBlockContent_Preview(@PreviewParameter(PreviewProvider::class) params: YieldSupplyUM) {
    TangemThemePreview {
        YieldSupplyBlockContent(yieldSupplyUM = params, modifier = Modifier)
    }
}

private class PreviewProvider : PreviewParameterProvider<YieldSupplyUM> {
    override val values: Sequence<YieldSupplyUM>
        get() = sequenceOf(
            YieldSupplyUM.Available(
                title = TextReference.Res(
                    R.string.yield_module_token_details_earn_notification_title,
                    wrappedList("5.1"),
                ),
                apy = "5.1",
                onClick = {},
            ),
            YieldSupplyUM.Content(
                title = stringReference("Aave lending is active"),
                subtitle = stringReference("Interest accrues automatically"),
                rewardsApy = stringReference("5.1 % APY"),
                onClick = {},
                apy = "5.1",
                showWarningIcon = false,
            ),
            YieldSupplyUM.Content(
                title = stringReference("Aave lending is active"),
                subtitle = stringReference("Interest accrues automatically"),
                rewardsApy = stringReference("5.1 % APY"),
                onClick = {},
                apy = "5.1",
                showWarningIcon = true,
            ),
            YieldSupplyUM.Loading,
            YieldSupplyUM.Processing.Enter,
            YieldSupplyUM.Processing.Exit,
            YieldSupplyUM.Unavailable,
        )
}
// endregion