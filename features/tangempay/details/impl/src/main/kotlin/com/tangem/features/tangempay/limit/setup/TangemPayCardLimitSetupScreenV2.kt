package com.tangem.features.tangempay.limit.setup

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayCardLimitSetupScreenV2(state: TangemPayCardLimitSetupUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TangemTopBar(
                modifier = Modifier.statusBarsPadding(),
                startContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_back_28),
                        onClick = state.onBackClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
                title = resourceReference(R.string.tangempay_card_page_daily_limit_title),
            )
        },
        containerColor = TangemTheme.colors3.bg.secondary,
    ) { scaffoldPaddings ->
        Content(
            state = state,
            modifier = Modifier.padding(scaffoldPaddings),
        )
    }
}

@Composable
private fun Content(state: TangemPayCardLimitSetupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x3)
            .imePadding(),
    ) {
        AmountBlock(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
            state = state,
        )
        Spacer(modifier = Modifier.weight(1f))
        PresetsRow(presets = state.presets)
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4)
                .padding(top = TangemTheme.dimens2.x3),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_daily_limit_set_button),
            onClick = state.onSubmitClick,
            isEnabled = state.isSubmitButtonEnabled,
            isLoading = state.isSubmitButtonLoading,
        )
    }
}

@Composable
private fun AmountBlock(state: TangemPayCardLimitSetupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
    ) {
        Text(
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.tertiary,
        )
        if (state.isInitialDataLoading) {
            TextShimmer(
                style = TextShimmerStyle.HEADING_MEDIUM,
                text = "$ 10000",
                radius = TangemTheme.dimens2.x25,
            )
        } else {
            AmountTextField(
                value = state.amountFieldModel.value,
                decimals = state.amountFieldModel.decimals,
                onValueChange = state.amountFieldModel.onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = AmountVisualTransformation(
                    decimals = state.amountFieldModel.decimals,
                    symbol = state.currencyCode,
                    currencyCode = state.currencyCode,
                    decimalFormat = rememberDecimalFormat(),
                    symbolColor = if (state.amountFieldModel.value.isBlank()) {
                        TangemTheme.colors3.text.tertiary
                    } else {
                        TangemTheme.colors3.text.primary
                    },
                ),
                textStyle = TangemTheme.typography3.display.medium.copy(
                    textAlign = TextAlign.Center,
                ),
                isAutoResize = true,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            )
        }
    }
}

@Composable
private fun PresetsRow(presets: ImmutableList<TangemPayCardLimitSetupUM.LimitPresetUM>) {
    if (presets.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x3, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        presets.forEach { preset ->
            PresetChip(
                preset = preset,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PresetChip(preset: TangemPayCardLimitSetupUM.LimitPresetUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors3.bg.tertiary)
            .clickable(onClick = preset.onClick)
            .padding(horizontal = TangemTheme.dimens2.x5, vertical = TangemTheme.dimens2.x1)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 1.dp)
                .fillMaxWidth(),
            text = preset.label,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreviewRedesign {
    TangemPayCardLimitSetupScreenV2(
        state = TangemPayCardLimitSetupUM.stub(),
    )
}