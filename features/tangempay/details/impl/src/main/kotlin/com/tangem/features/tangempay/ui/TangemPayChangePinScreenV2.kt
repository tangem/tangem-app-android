package com.tangem.features.tangempay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import kotlinx.coroutines.delay

@Composable
internal fun TangemPayChangePinScreenV2(
    state: TangemPayChangePinUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TangemTopBar(
            title = resourceReference(R.string.visa_onboarding_pin_code_title),
            endContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_cross_20),
                    onClick = onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        SpacerH(TangemTheme.dimens2.x4)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens2.x12)
                .padding(horizontal = TangemTheme.dimens2.x9),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.visa_onboarding_pin_code_description),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(TangemPayTestTags.PIN_SCREEN_TITLE),
            )
            SpacerH(TangemTheme.dimens2.x6)
            PinCodeSection(state)
            SpacerH(TangemTheme.dimens2.x6)
            AnimatedVisibility(state.submitButtonLoading) {
                TangemLoader()
            }
        }
    }
}

@Composable
private fun PinCodeSection(state: TangemPayChangePinUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        PinCode(
            isError = state.error != null,
            value = state.pinCode,
            onValueChange = state.onPinCodeChange,
            focusRequester = focusRequester,
            readOnly = state.submitButtonLoading,
        )

        AnimatedVisibility(
            visible = state.error != null,
        ) {
            val error = remember(this) { requireNotNull(state.error) }
            Column {
                SpacerH4()
                Text(
                    text = error.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.status.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(TangemPayTestTags.PIN_ERROR_MESSAGE),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(timeMillis = 300)
        focusRequester.requestFocus()
    }
}

@Composable
private fun PinCode(
    isError: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numbersCount: Int = 4,
    readOnly: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Hide the keyboard while the PIN is being submitted so it can't be edited mid-request.
    LaunchedEffect(readOnly) {
        if (readOnly) keyboardController?.hide()
    }

    BasicTextField(
        value = value,
        onValueChange = { text ->
            if (text.length <= numbersCount && !readOnly) {
                onValueChange(text)
            }
        },
        readOnly = readOnly,
        modifier = modifier
            .focusRequester(focusRequester)
            .clickable(enabled = !readOnly) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .testTag(TangemPayTestTags.PIN_INPUT_FIELD),
        textStyle = TangemTheme.typography3.heading.medium.copy(color = Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        cursorBrush = SolidColor(Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(numbersCount) { index ->
                        val digit = value.getOrNull(index)?.toString()
                        val isActive = !readOnly && index == value.length
                        PinDigitBox(
                            modifier = Modifier.size(
                                width = TangemTheme.dimens2.x14,
                                height = TangemTheme.dimens2.x16,
                            ),
                            digit = digit,
                            backgroundColor = TangemTheme.colors3.bg.opaque.primary,
                            borderColor = when {
                                isError -> TangemTheme.colors3.border.status.error
                                isActive -> TangemTheme.colors3.border.status.info
                                else -> TangemTheme.colors3.border.secondary
                            },
                            textColor = TangemTheme.colors3.text.primary,
                            textStyle = TangemTheme.typography3.heading.medium,
                        )
                    }
                }
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TangemPayChangePinScreenV2Preview(
    @PreviewParameter(TangemPayChangePinUMPreviewProvider::class) state: TangemPayChangePinUM,
) {
    TangemThemePreviewRedesign {
        TangemPayChangePinScreenV2(
            state = state,
            onBackClick = {},
        )
    }
}

private class TangemPayChangePinUMPreviewProvider : CollectionPreviewParameterProvider<TangemPayChangePinUM>(
    collection = listOf(
        TangemPayChangePinUM(
            pinCode = "",
            error = null,
            onPinCodeChange = {},
            submitButtonLoading = false,
            submitButtonEnabled = false,
            onSubmitClick = {},
        ),
        TangemPayChangePinUM(
            pinCode = "1111",
            error = resourceReference(R.string.visa_onboarding_pin_validation_error_message),
            onPinCodeChange = {},
            submitButtonLoading = false,
            submitButtonEnabled = false,
            onSubmitClick = {},
        ),
        TangemPayChangePinUM(
            pinCode = "2580",
            error = null,
            onPinCodeChange = {},
            submitButtonLoading = false,
            submitButtonEnabled = true,
            onSubmitClick = {},
        ),
    ),
)