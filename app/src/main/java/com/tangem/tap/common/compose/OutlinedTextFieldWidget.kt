package com.tangem.tap.common.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.domain.common.DomainError
import com.tangem.domain.common.ErrorConverter

/**
[REDACTED_AUTHOR]
 */
private class OutlinedTextFieldWidget

@Composable
fun OutlinedTextFieldWidget(
    text: String,
    modifier: Modifier = Modifier,
    labelId: Int? = null,
    label: String = "",
    placeholderId: Int? = null,
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    isLoading: Boolean = false,
    error: DomainError? = null,
    errorConverter: ErrorConverter<String>? = null,
    debounceTextChanges: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextChanged: (String) -> Unit,
) {
    if (!isVisible) return

    val placeholder = placeholderId?.let { stringResource(id = it) } ?: placeholder
    val label = labelId?.let { stringResource(id = it) } ?: label

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        OutlinedProgressTextField(
            text = text,
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            isEnabled = isEnabled,
            isLoading = isLoading,
            error = error,
            debounceTextChanges = debounceTextChanges,
            visualTransformation = visualTransformation,
            onTextChanged = onTextChanged
        )
        errorConverter?.let { TextFieldErrorWidget(error, it) }
    }
}

@Composable
private fun OutlinedProgressTextField(
    text: String,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    error: DomainError? = null,
    debounceTextChanges: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTextChanged: (String) -> Unit,
) {
    val rTextValue = remember { mutableStateOf(text) }
    val textDebouncer = ComposableTextDebouncer(text, debounceTextChanges, onTextChanged)

    // add ability to paste text from state
    if (rTextValue.value != text) rTextValue.value = text

    Box {
        OutlinedTextField(
            value = rTextValue.value,
            onValueChange = {
                // immediately change text for the OutlinedTextField
                rTextValue.value = it
                textDebouncer.emmit(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
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
private fun TextFieldErrorWidget(
    error: DomainError? = null,
    errorConverter: ErrorConverter<String>,
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        error?.let {
            ErrorView(errorConverter.convertError(it), style = TextStyle(fontSize = 14.sp))
        }
    }
}

@Preview
@Composable
fun OutlinedTextFieldWithErrorTest() {
    val converter = remember {
        object : ErrorConverter<String> {
            override fun convertError(error: DomainError): String {
                return "Hello, i'am the error: ${error::class.java.simpleName}"
            }

        }
    }

    class SimpleError(
        override val code: Int = 1,
        override val message: String = "Error message",
        override val data: Any? = null,
    ) : DomainError

    val modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    Scaffold(
    ) {
        Column(
        ) {
            OutlinedTextFieldWidget(
                modifier = modifier,
                text = "",
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
                onTextChanged = {},
            )
            OutlinedTextFieldWidget(
                modifier = modifier,
                text = "First",
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
                onTextChanged = {},
            )
            OutlinedTextFieldWidget(
                modifier = modifier,
                text = "First",
                label = "First label",
                placeholder = "1 placeholder",
                isLoading = true,
                error = null,
                errorConverter = converter,
                onTextChanged = {},
            )
            OutlinedTextFieldWidget(
                modifier = modifier,
                text = "First",
                label = "First label",
                placeholder = "1 placeholder",
                error = SimpleError(),
                errorConverter = converter,
                onTextChanged = {},
            )
        }
    }
}