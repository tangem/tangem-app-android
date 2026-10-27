package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountPresetUM
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PreviewAmountPresets
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PredictionAmountPresets(
    presets: ImmutableList<AmountPresetUM>,
    isEnabled: Boolean,
    onPresetClick: (AmountPresetUM) -> Unit,
    onMaxClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PresetsPadding,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        items(items = presets, key = { it.amount }) { preset ->
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = stringReference(preset.label),
                isEnabled = isEnabled,
                onClick = { onPresetClick(preset) },
            )
        }
        item(key = MAX_KEY) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = resourceReference(R.string.send_max_amount),
                isEnabled = isEnabled,
                onClick = onMaxClick,
            )
        }
    }
}

private val PresetsPadding = PaddingValues(horizontal = 16.dp)
private const val MAX_KEY = "max"

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionAmountPresetsPreview() {
    TangemThemePreviewRedesign {
        PredictionAmountPresets(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
            presets = PreviewAmountPresets,
            isEnabled = true,
            onPresetClick = {},
            onMaxClick = {},
        )
    }
}