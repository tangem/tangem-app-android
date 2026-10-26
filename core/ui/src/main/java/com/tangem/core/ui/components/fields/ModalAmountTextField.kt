package com.tangem.core.ui.components.fields

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.utils.rememberDecimalFormat

/**
 * [AmountTextField] that supports intrinsic measurements, for content of
 * [com.tangem.core.ui.ds2.modal.TangemModal]: the modal probes its content's intrinsic height, which
 * throws on the `BoxWithConstraints` inside [AmountTextField]. Parameters are identical.
 *
 * Hiding the keyboard clears the field's focus: the modal stays expanded for as long as its content holds focus,
 * and dismissing the keyboard (back gesture, IME button) does not clear it on its own.
 *
 * @see AmountTextField
 */
@Composable
fun ModalAmountTextField(
    value: String,
    decimals: Int,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    colors: AmountTextFieldColors = TangemAmountTextFieldColors,
    visualTransformation: VisualTransformation = AmountVisualTransformation(
        decimals = decimals,
        symbolColor = if (value.isBlank()) colors.disabledTextColor else colors.textColor,
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isEnabled: Boolean = true,
    isAutoResize: Boolean = false,
    isValuePasted: Boolean = false,
    onValuePastedTriggerDismiss: () -> Unit = {},
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    reduceFactor: Double = 0.9,
) {
    val decimalFormat = rememberDecimalFormat()
    ClearFocusOnKeyboardHidden()
    var maxWidthPx by remember { mutableIntStateOf(NOT_MEASURED) }
    Box(modifier = modifier.then(MaxWidthReporterElement(onMaxWidth = { maxWidthPx = it }))) {
        val fontSize = if (isAutoResize && maxWidthPx != NOT_MEASURED) {
            resizeFont(
                maxWidth = maxWidthPx.toMaxWidthDp(),
                visualTransformation = visualTransformation,
                value = value,
                textStyle = textStyle,
                reduceFactor = reduceFactor,
            )
        } else {
            textStyle.fontSize
        }
        val textColor = if (value.isBlank()) colors.disabledTextColor else colors.textColor
        SimpleTextField(
            value = value,
            onValueChange = { newText ->
                onValueChange(
                    prepareEnter(
                        oldValue = value,
                        newValue = newText,
                        decimalFormat = decimalFormat,
                        decimals = decimals,
                    ),
                )
            },
            textStyle = textStyle.copy(
                fontSize = fontSize,
                textDirection = TextDirection.ContentOrLtr,
            ),
            isValuePasted = isValuePasted,
            onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
            color = textColor,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            readOnly = !isEnabled,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .background(colors.backgroundColor)
                .testTag(SendScreenTestTags.INPUT_TEXT_FIELD),
        )
    }
}

private const val NOT_MEASURED = -1

@Composable
private fun ClearFocusOnKeyboardHidden() {
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible by rememberIsKeyboardVisible()
    var wasKeyboardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            wasKeyboardVisible = true
        } else if (wasKeyboardVisible) {
            wasKeyboardVisible = false
            focusManager.clearFocus()
        }
    }
}

@Composable
private fun Int.toMaxWidthDp(): Dp {
    return if (this == Constraints.Infinity) Dp.Infinity else with(LocalDensity.current) { toDp() }
}

private data class MaxWidthReporterElement(
    val onMaxWidth: (Int) -> Unit,
) : ModifierNodeElement<MaxWidthReporterNode>() {

    override fun create(): MaxWidthReporterNode = MaxWidthReporterNode(onMaxWidth)

    override fun update(node: MaxWidthReporterNode) {
        node.onMaxWidth = onMaxWidth
    }
}

/**
 * Reports the incoming max width — what `BoxWithConstraints.maxWidth` exposes — without subcomposition.
 * Intrinsic queries pass straight through: the default ones would run [measure] with probe constraints
 * and report the probe width, flipping the state between it and the real width on every pass.
 */
private class MaxWidthReporterNode(var onMaxWidth: (Int) -> Unit) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        onMaxWidth(constraints.maxWidth)
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(x = 0, y = 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}

// region preview
@Preview(widthDp = 360, showBackground = true)
@Composable
private fun ModalAmountTextFieldPreview() {
    var text by remember { mutableStateOf("1000000000000.123456") }
    TangemThemePreview {
        // Intrinsic height is what TangemModal probes; AmountTextField throws here.
        Column(modifier = Modifier.height(IntrinsicSize.Min)) {
            ModalAmountTextField(
                value = text,
                decimals = 6,
                onValueChange = { text = it },
                textStyle = TangemTheme.typography.h2.copy(
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                ),
                isAutoResize = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
// endregion