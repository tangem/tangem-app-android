package com.tangem.tap.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly

/**
 * Created by Anton Zhilenkov on 30.09.2022.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinCodeWidget(
    config: PinViewConfig = tangemPinConfig,
    onPinChange: (String, Boolean) -> Unit = { pin, isLastSymbolEntered -> },
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val rTextFieldValue = remember { mutableStateOf(TextFieldValue("")) }
    val indexedSymbols: List<String?> = createPinSymbolsList(config.pinsCount, rTextFieldValue.value.text)

    fun isLastSymbolEntered(): Boolean = rTextFieldValue.value.text.length == config.pinsCount

    fun handleOnTextFieldValueChanged(value: TextFieldValue) {
        if (!value.text.isDigitsOnly()) return

        if (value.text.length <= config.pinsCount) {
            rTextFieldValue.value = value
            onPinChange(value.text, isLastSymbolEntered())
        }
    }

    Box(
        modifier = config.modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
    ) {
        Row {
            for (index in 0 until config.pinsCount) {
                PinElement(
                    config = config,
                    pinSymbol = indexedSymbols[index] ?: "",
                )
            }
        }
        TextField(
            modifier = Modifier
                .alpha(0f)
                .size(1.dp)
                .align(Alignment.Center)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = if (isLastSymbolEntered()) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() },
            ),
            value = rTextFieldValue.value,
            onValueChange = ::handleOnTextFieldValueChanged,
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun PinElement(
    config: PinViewConfig,
    pinSymbol: String,
) {
    Box(Modifier.padding(config.pinBoxPadding)) {
        Box(config.pinBoxModifier) {
            Text(
                text = pinSymbol,
                modifier = config.pinTextModifier.align(Alignment.Center),
                style = config.pinsTextStyle ?: LocalTextStyle.current,
            )
        }
    }
}

private fun createPinSymbolsList(size: Int, text: String): List<String?> = List(size) {
    try {
        text[it].toString()
    } catch (ex: IndexOutOfBoundsException) {
        null
    }
}

data class PinViewConfig(
    val modifier: Modifier = Modifier,
    val pinBoxModifier: Modifier = Modifier,
    val pinBoxPadding: Dp = 0.dp,
    val pinTextModifier: Modifier = Modifier,
    val pinsCount: Int = 4,
    val pinsTextStyle: TextStyle? = null,
)

private val tangemPinConfig = PinViewConfig(
    modifier = Modifier
        .wrapContentSize(),
    pinBoxModifier = Modifier
        .width(42.dp)
        .height(56.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFFF0F0F0)),
    pinBoxPadding = 6.dp,
    pinTextModifier = Modifier,
    pinsCount = 4,
    pinsTextStyle = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 24.sp,
    ),
)

@Preview
@Composable
private fun PinCodeWidgetPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        PinCodeWidget(tangemPinConfig)
    }
}
