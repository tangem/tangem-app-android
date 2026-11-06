package com.tangem.features.earn.impl.widgets.calculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_double_vertical_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.widgets.calculator.model.CalculatorInput
import com.tangem.features.earn.impl.widgets.calculator.model.EarnCalculatorConverter
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnCalculatorUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnHorizonSelectUM
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import java.math.BigDecimal

@Composable
internal fun EarnCalculatorContent(state: EarnCalculatorUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.incentive?.let { badge ->
            TangemBadge(text = badge.text, variant = badge.variant, status = badge.status, size = badge.size)
            Spacer(modifier = Modifier.height(12.dp))
        }
        Headline(text = state.total.resolveReference())
        Spacer(modifier = Modifier.height(8.dp))
        EarnedRow(state = state)
        Spacer(modifier = Modifier.height(24.dp))
        EarnProjectionBars(bars = state.bars, axisLabels = state.axisLabels)
        Spacer(modifier = Modifier.height(24.dp))
        EarnStepper(state = state.initialBalance)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TangemTheme.colors3.border.primary),
        )
        EarnStepper(state = state.monthlyTopUp)
    }
}

@Composable
private fun Headline(text: String) {
    val style = TangemTheme.typography3.display.medium

    BasicText(
        text = text,
        style = style.copy(color = TangemTheme.colors3.text.primary),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = HEADLINE_MIN_FONT_SIZE, maxFontSize = style.fontSize),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EarnedRow(state: EarnCalculatorUM) {
    val textStyle: TextStyle = TangemTheme.typography3.subheading.medium

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.earned.resolveReference(),
            style = textStyle,
            color = TangemTheme.colors3.text.brand,
        )
        Text(
            text = state.earnedLabel.resolveReference(),
            style = textStyle,
            color = TangemTheme.colors3.text.secondary,
        )
        HorizonPicker(horizon = state.horizon, textStyle = textStyle)
    }
}

@Composable
private fun HorizonPicker(horizon: EarnHorizonSelectUM, textStyle: TextStyle) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) { isMenuExpanded = true }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = horizon.selected.resolveReference(),
                style = textStyle,
                color = TangemTheme.colors3.text.primary,
            )
            Icon(
                imageVector = Icons.ic_chevron_double_vertical_16,
                contentDescription = null,
                tint = TangemTheme.colors3.text.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        TangemMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.hazeEffectTangem { blurRadius = 10.dp },
            isSelectable = true,
        ) {
            horizon.items.forEach { item ->
                TangemMenu.Item(
                    label = item.label,
                    isSelected = item.isSelected,
                    onClick = {
                        isMenuExpanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

private val HEADLINE_MIN_FONT_SIZE = 20.sp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnCalculatorContentPreview() {
    val input = CalculatorInput.Default
    val result = DefaultEarnProjection(netRate = BigDecimal("0.0468"))
        .project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

    TangemThemePreviewRedesign {
        EarnCalculatorContent(
            state = EarnCalculatorConverter().convert(
                result = result,
                input = input,
                appCurrency = AppCurrency.Default,
                incentiveRate = EarnProductRate(fraction = BigDecimal("0.014"), type = EarnRateType.APY),
                onInitialStep = {},
                onTopUpStep = {},
                onHorizonSelect = {},
            ),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
        )
    }
}