package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingFooter
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.AmountTextFieldColors
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SLIPPAGE_DECIMALS
import com.tangem.features.polymarket.impl.placeprediction.entity.SLIPPAGE_PRESETS
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippagePickerUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippagePresetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.label
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSlippageIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewProvider

private val ChipShape = RoundedCornerShape(percent = 50)

private val TitleToValueGap = 68.dp
private val ValueToChipsGap = 32.dp
private val ChipsToDescriptionGap = 32.dp

@Composable
internal fun PlacePredictionSlippageContent(
    state: SlippagePickerUM,
    intents: PlacePredictionSlippageIntents,
    modifier: Modifier = Modifier,
) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_place_slippage),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = intents::onBackClick,
            )
        },
        overlay = { contentPadding ->
            OnboardingFooter(
                contentPadding = contentPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_confirm),
                    isEnabled = state.isConfirmEnabled,
                    onClick = intents::onSlippageConfirmClick,
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Spacer(modifier = Modifier.height(TitleToValueGap))
            ValueSection(state = state, onValueChange = intents::onSlippageValueChange)
            Spacer(modifier = Modifier.height(ValueToChipsGap))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                items(items = SLIPPAGE_PRESETS, key = { it.percent.toPlainString() }) { preset ->
                    PresetChip(preset = preset, onClick = { intents.onSlippagePresetClick(preset.percent) })
                }
            }
            Spacer(modifier = Modifier.height(ChipsToDescriptionGap))
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResourceSafe(R.string.prediction_place_slippage_sheet_description),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun ValueSection(state: SlippagePickerUM, onValueChange: (String) -> Unit) {
    val valueColor = if (state.value.isBlank()) {
        TangemTheme.colors3.text.tertiary
    } else {
        TangemTheme.colors3.text.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        AmountTextField(
            value = state.value,
            decimals = SLIPPAGE_DECIMALS,
            onValueChange = onValueChange,
            textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
            colors = AmountTextFieldColors(
                textColor = valueColor,
                disabledTextColor = valueColor,
                backgroundColor = Color.Transparent,
            ),
            isAutoResize = true,
            visualTransformation = PercentVisualTransformation,
        )
        state.error?.let { error ->
            Text(
                text = error.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PresetChip(preset: SlippagePresetUM, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(ChipShape)
            .background(TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = preset.label().resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
    }
}

private object PercentVisualTransformation : VisualTransformation {

    private const val PERCENT_SIGN = "%"
    private const val EMPTY_HINT = "0"

    override fun filter(text: AnnotatedString): TransformedText {
        val shown = text.text.ifEmpty { EMPTY_HINT }

        return TransformedText(
            text = buildAnnotatedString {
                append(shown)
                append(PERCENT_SIGN)
            },
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, shown.length)
                override fun transformedToOriginal(offset: Int): Int = offset.coerceIn(0, text.text.length)
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionSlippageContentPreview(
    @PreviewParameter(PlacePredictionPreviewProvider::class) state: PlacePredictionUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionSlippageContent(
            state = SlippagePickerUM.of(state.slippage.percent.toPlainString()),
            intents = PlacePredictionPreviewIntents,
        )
    }
}