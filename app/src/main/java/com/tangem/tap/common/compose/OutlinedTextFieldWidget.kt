package com.tangem.tap.common.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.module.ModuleError
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.CompositionLogger
import com.tangem.tap.common.compose.extensions.stringResourceDefault
import com.tangem.tap.common.moduleMessage.ModuleMessageConverter

/**
[REDACTED_AUTHOR]
 */
@Composable
fun OutlinedTextFieldWidget(
    modifier: Modifier = Modifier,
    fieldData: Field.Data<String>,
    labelId: Int? = null,
    label: String = "",
    placeholderId: Int? = null,
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    isLoading: Boolean = false,
    error: ModuleError? = null,
    errorConverter: ModuleMessageConverter? = null,
    debounceTextChanges: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onTextChanged: (String) -> Unit,
) {
    if (!isVisible) return

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        OutlinedProgressTextField(
            modifier = modifier,
            fieldData = fieldData,
            label = stringResourceDefault(labelId, label),
            placeholder = stringResourceDefault(placeholderId, placeholder),
            trailingIcon = trailingIcon,
            isEnabled = isEnabled,
            isLoading = isLoading,
            error = error,
            debounce = debounceTextChanges,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            onTextChanged = onTextChanged
        )
        errorConverter?.let { AnimatedErrorView(error, it) }
    }
}

@Composable
private fun OutlinedProgressTextField(
    modifier: Modifier = Modifier,
    fieldData: Field.Data<String>,
    label: String = "",
    placeholder: String = "",
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    error: ModuleError? = null,
    debounce: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTextChanged: (String) -> Unit,
) {
    val logger = remember {
        CompositionLogger(label, "OutlinedProgressTextField", listOf("Символ токена"))
    }
    logger.nextComposition()

    val textValueState = remember { mutableStateOf(fieldData.value) }
    val textDebouncer = valueDebouncerAsState(
        initialValue = fieldData.value,
        debounce = debounce,
        onEmitValueReceived = {
            logger.log("DEBOUNCER: onEmitValueReceived: [$it]")
            logger.log("DEBOUNCER: start RECOMPOSE by new value for textValueState.value = [$it]")
            textValueState.value = it
        },
        onValueChanged = {
            logger.log("DEBOUNCER: onValueChanged:  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  dispatch.toStore([$it])")
            onTextChanged(it)
        })

    logger.log("RECOMPOSE ---------------------------------------------------------------START [${logger.count}]")
    logger.log("RECOMPOSE --data: fieldData.value: [${fieldData}]")
    logger.log("RECOMPOSE --data: textValueState.value: [${textValueState.value}]")
    logger.log("RECOMPOSE --data: textDebouncer.emittedValue = [${textDebouncer.emittedValue}]")
    logger.log("RECOMPOSE --data: textDebouncer.debounced = [${textDebouncer.debounced}]")

    if (!fieldData.isUserInput) {
        // initial value is not from an user
        val isNotUserInput = "-- IS NOT USER INPUT"
        logger.log("recompose $isNotUserInput")
        if (textValueState.value == fieldData.value) {
            logger.log("$isNotUserInput: внешние данные ОДИНАКОВЫ с данными в поле")
        } else {
            logger.log("$isNotUserInput: внешние данные РАЗЛИЧАЮТСЯ с данными в поле")
            if ((textDebouncer.emittedValue != textDebouncer.debounced) || textDebouncer.emitsCountBeforeDebounce > 0) {
                logger.log("$isNotUserInput: пользователь ВВОДИТ данные -> внешние данные игнорируем, ждем RECOMPOSE")
            } else {
                logger.log("$isNotUserInput: пользователь НЕ вводит данные -> пытаемся обработать внешние данные")
                if (textValueState.value != textDebouncer.emittedValue || textValueState.value != textDebouncer.debounced) {
                    logger.log("$isNotUserInput: даннные в поле не соответствуют данным из textDebouncer")
                    if (textDebouncer.emittedValue.isEmpty() && textDebouncer.debounced.isEmpty()) {
                        logger.log("$isNotUserInput: даннные в textDebouncer ПУСТЫ -> start RECOMPOSE новые данные для textValueState.value = [${fieldData.value}]")
                        textValueState.value = fieldData.value
                    } else {
                        logger.log("$isNotUserInput: даннные в textDebouncer НЕ ПУСТЫ  -> start RECOMPOSE новые данные для textValueState.value = [${fieldData.value}]")
                        textValueState.value = fieldData.value
                    }
                } else {
                    logger.log("$isNotUserInput: в пустое поле вставляются данные -> start RECOMPOSE новые данные для textValueState.value = [${fieldData.value}]")
                    textValueState.value = fieldData.value
                }
            }
        }
    }
    logger.log("recompose --------------------------------------------------------------FINISH [${logger.count}]")

    Box {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = textValueState.value,
            onValueChange = {
                logger.log("WIDGET: textDebouncer.emmit([$it])")
                textDebouncer.emmit(it)
            },
            keyboardOptions = keyboardOptions,
            label = { Text(label) },
            placeholder = {
                Text(
                    text = placeholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            enabled = isEnabled,
            isError = error != null,
            visualTransformation = visualTransformation,
        )
        AnimatedVisibility(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 6.dp, top = 0.dp, end = 6.dp, bottom = 6.dp),
            visible = isLoading,
        ) { LinearProgressIndicator() }
    }
}

@Composable
private fun AnimatedErrorView(
    error: ModuleError? = null,
    errorConverter: ModuleMessageConverter,
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        error?.let {
            ErrorView(errorConverter.convert(it), style = TextStyle(fontSize = 14.sp))
        }
    }
}

@Preview
@Composable
fun OutlinedTextFieldWithErrorTest() {
    val context = LocalContext.current
    val converter = remember { ModuleMessageConverter(context) }

    class SimpleError(
        override val code: Int = 1,
        override val message: String = "Error message",
        override val data: Any? = null,
    ) : ModuleError

    val modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    Scaffold(
    ) {
        Column(
        ) {
            OutlinedTextFieldWidget(
                modifier = modifier,
                fieldData = Field.Data("", false),
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                fieldData = Field.Data("First", false),
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                fieldData = Field.Data("First", false),
                label = "First label",
                placeholder = "1 placeholder",
                isLoading = true,
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                fieldData = Field.Data("First", false),
                label = "First label",
                placeholder = "1 placeholder",
                error = SimpleError(),
                errorConverter = converter,
            ) {}
        }
    }
}