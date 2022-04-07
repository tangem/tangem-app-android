package com.tangem.tap.common.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    isEnabled: Boolean = true,
    error: DomainError? = null,
    errorConverter: ErrorConverter<String>? = null,
    debounceTextChanges: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextChanged: (String) -> Unit,
) {
    val placeholder = placeholderId?.let { stringResource(id = it) } ?: placeholder
    val label = labelId?.let { stringResource(id = it) } ?: label

    val rTextValue = remember { mutableStateOf(text) }
    val textDebouncer = ComposableTextDebouncer(text, debounceTextChanges, onTextChanged)

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        OutlinedTextField(
            value = rTextValue.value,
            onValueChange = {
                rTextValue.value = it
                textDebouncer.emmit(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            enabled = isEnabled,
            isError = error != null,
            visualTransformation = visualTransformation,
        )
        errorConverter?.let { TextFieldErrorWidget(error, it) }
    }
}

@Composable
fun TextFieldErrorWidget(
    error: DomainError? = null,
    errorConverter: ErrorConverter<String>,
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        ErrorView(
            errorConverter.convertError(error!!),
            style = TextStyle(
                fontSize = 14.sp
            )
        )
    }
}

@Composable
fun ErrorView(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text,
        color = MaterialTheme.colors.error,
        modifier = modifier,
        style = style
    )
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
                error = SimpleError(),
                errorConverter = converter,
                onTextChanged = {},
            )
        }
    }
}