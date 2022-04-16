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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.domain.DomainError
import com.tangem.domain.ErrorConverter
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.compose.extensions.stringResourceDefault

/**
[REDACTED_AUTHOR]
 */
@Composable
fun OutlinedTextFieldWidget(
    modifier: Modifier = Modifier,
    textFieldData: Field.Data<String>,
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onTextChanged: (String) -> Unit,
) {
    if (!isVisible) return

    Column(
        modifier = modifier.animateContentSize(),
    ) {
        OutlinedProgressTextField(
            modifier = modifier,
            textFieldData = textFieldData,
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
    textFieldData: Field.Data<String>,
    label: String = "",
    placeholder: String = "",
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    error: DomainError? = null,
    debounce: Long = 400,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTextChanged: (String) -> Unit,
) {
    val rTextDebouncer = valueDebouncerAsState(debounce, onTextChanged)
    val rText = remember { mutableStateOf(textFieldData.value) }

    fun updateFieldValueAndEmmit(value: String) {
        rText.value = value
        rTextDebouncer.emmit(value)
    }
    // This action came from redux. Update the field value and send a new event as if from the user
    if (!textFieldData.isUserInput) {
        updateFieldValueAndEmmit(textFieldData.value)
    }

    Box {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = rText.value,
            onValueChange = ::updateFieldValueAndEmmit,
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
                textFieldData = Field.Data(""),
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                textFieldData = Field.Data("First"),
                label = "First label",
                placeholder = "1 placeholder",
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                textFieldData = Field.Data("First"),
                label = "First label",
                placeholder = "1 placeholder",
                isLoading = true,
                error = null,
                errorConverter = converter,
            ) {}
            OutlinedTextFieldWidget(
                modifier = modifier,
                textFieldData = Field.Data("First"),
                label = "First label",
                placeholder = "1 placeholder",
                error = SimpleError(),
                errorConverter = converter,
            ) {}
        }
    }
}