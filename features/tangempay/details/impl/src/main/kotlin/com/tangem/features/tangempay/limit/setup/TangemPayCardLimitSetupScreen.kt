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
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayCardLimitSetupScreen(state: TangemPayCardLimitSetupUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = stringResourceSafe(R.string.tangempay_card_page_daily_limit_title),
                startButton = TopAppBarButtonUM.Close(onCloseClick = state.onBackClick),
            )
        },
        containerColor = TangemTheme.colors.background.secondary,
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
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountBlock(
            modifier = Modifier.padding(horizontal = 16.dp),
            state = state,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResourceSafe(R.string.tangempay_daily_limit_set_button),
            enabled = state.isSubmitButtonEnabled,
            showProgress = state.isSubmitButtonLoading,
            onClick = state.onSubmitClick,
        )
        PresetsRow(presets = state.presets)
    }
}

@Composable
private fun AmountBlock(state: TangemPayCardLimitSetupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action)
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_amount),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        SpacerH12()
        if (state.isInitialDataLoading) {
            TextShimmer(
                style = TangemTheme.typography.head,
                text = "$5000",
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
                        TangemTheme.colors.text.disabled
                    } else {
                        TangemTheme.colors.text.primary1
                    },
                ),
                textStyle = TangemTheme.typography.head.copy(
                    textAlign = TextAlign.Center,
                ),
                isAutoResize = true,
            )
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun PresetsRow(presets: ImmutableList<TangemPayCardLimitSetupUM.LimitPresetUM>) {
    if (presets.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.button.secondary)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = preset.onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = preset.label,
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreview {
    TangemPayCardLimitSetupScreen(
        state = TangemPayCardLimitSetupUM.stub(),
    )
}