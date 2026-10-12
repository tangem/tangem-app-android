package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.fields.AmountTextFieldColors
import com.tangem.core.ui.components.fields.ModalAmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.PercentVisualTransformation
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_24
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_24
import com.tangem.feature.swap.model.SwapSlippage
import com.tangem.feature.swap.models.states.SwapSlippageBottomSheetConfig
import com.tangem.feature.swap.models.states.SwapSlippagePresetUM
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SwapSlippageContent(state: SwapSlippageBottomSheetConfig, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SlippageValue(content = state)
        Column(
            modifier = Modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoLine(
                icon = Icons.ic_percent_24,
                text = annotatedReference {
                    appendColored(
                        text = stringResourceSafe(R.string.common_slippage),
                        color = TangemTheme.colors3.text.primary,
                    )
                    appendSpace()
                    append(stringResourceSafe(R.string.swap_slippage_info_text))
                },
            )
            InfoLine(
                icon = Icons.ic_sign_usd_24,
                text = annotatedReference {
                    appendColored(
                        text = stringResourceSafe(R.string.swap_exchange_rates_info_title),
                        color = TangemTheme.colors3.text.primary,
                    )
                    appendSpace()
                    append(stringResourceSafe(R.string.swap_exchange_rates_info_text))
                },
            )
        }
    }
}

@Composable
internal fun SwapSlippageFooter(state: SwapSlippageBottomSheetConfig) {
    TangemFooter(
        windowInsets = WindowInsets(0),
        fadeEnabled = false,
        blurBackground = false,
    ) {
        TangemButton(
            text = resourceReference(R.string.common_confirm),
            variant = TangemButton.Variant.Primary,
            isEnabled = when (val valueState = state.valueState) {
                is SwapSlippageBottomSheetConfig.Value.Edit -> valueState.isConfirmEnabled
                is SwapSlippageBottomSheetConfig.Value.ReadOnly -> true
            },
            onClick = state.onConfirmClick,
            size = TangemButton.Size.X12,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun SlippageValue(content: SwapSlippageBottomSheetConfig) {
    when (val valueState = content.valueState) {
        is SwapSlippageBottomSheetConfig.Value.Edit -> {
            val valueColor = if (valueState.value.isBlank()) {
                TangemTheme.colors3.text.tertiary
            } else {
                TangemTheme.colors3.text.primary
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.height(80.dp),
                ) {
                    ModalAmountTextField(
                        value = valueState.value,
                        onValueChange = valueState.onValueChange,
                        decimals = SwapSlippage.DECIMALS,
                        textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
                        colors = AmountTextFieldColors(
                            textColor = valueColor,
                            disabledTextColor = valueColor,
                            backgroundColor = Color.Unspecified,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = PercentVisualTransformation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val caption = when {
                        content.error != null -> content.error to TangemTheme.colors3.text.status.error
                        content.warning != null -> content.warning to TangemTheme.colors3.text.status.warning
                        else -> null
                    }

                    val displayedCaption = rememberLastNonNull(caption)
                    AnimatedVisibility(caption != null) {
                        if (displayedCaption != null) {
                            Text(
                                text = displayedCaption.first.resolveReference(),
                                style = TangemTheme.typography3.caption.medium,
                                color = displayedCaption.second,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    valueState.presets.fastForEach { preset ->
                        TangemButton(
                            text = preset.label,
                            variant = TangemButton.Variant.Secondary,
                            size = TangemButton.Size.X8,
                            onClick = preset.onClick,
                        )
                    }
                }
            }
        }
        is SwapSlippageBottomSheetConfig.Value.ReadOnly -> TangemMessageBanner(
            title = valueState.info,
            variant = TangemMessageBanner.Variant.Info,
        )
    }
}

@Composable
private fun InfoLine(text: TextReference, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapSlippageBottomSheetContent_Preview(
    @PreviewParameter(SwapSlippageBottomSheetPreviewProvider::class) params: SwapSlippageBottomSheetConfig,
) {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            SwapSlippageContent(state = params)
            SwapSlippageFooter(state = params)
        }
    }
}

private class SwapSlippageBottomSheetPreviewProvider : PreviewParameterProvider<SwapSlippageBottomSheetConfig> {
    private val editState = SwapSlippageBottomSheetConfig.Value.Edit(
        value = "0.3",
        onValueChange = {},
        isConfirmEnabled = true,
        presets = persistentListOf(
            SwapSlippagePresetUM(value = "0.15", label = stringReference("0.15%"), onClick = {}),
            SwapSlippagePresetUM(value = "0.3", label = stringReference("Default 0.3%"), onClick = {}),
            SwapSlippagePresetUM(value = "0.6", label = stringReference("0.6%"), onClick = {}),
        ),
    )

    override val values: Sequence<SwapSlippageBottomSheetConfig>
        get() = sequenceOf(
            SwapSlippageBottomSheetConfig(valueState = editState, error = null, onConfirmClick = {}),
            SwapSlippageBottomSheetConfig(
                valueState = editState,
                error = resourceReference(R.string.swap_slippage_error),
                onConfirmClick = {},
            ),
            SwapSlippageBottomSheetConfig(
                valueState = editState.copy(value = "0.11"),
                error = null,
                warning = resourceReference(R.string.swap_slippage_warning_text),
                onConfirmClick = {},
            ),
            SwapSlippageBottomSheetConfig(
                valueState = SwapSlippageBottomSheetConfig.Value.ReadOnly(stringReference("1.5%")),
                error = null,
                onConfirmClick = {},
            ),
        )
}
// endregion