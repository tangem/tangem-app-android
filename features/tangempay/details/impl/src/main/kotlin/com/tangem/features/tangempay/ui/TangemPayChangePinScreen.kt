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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import kotlinx.coroutines.delay

@Composable
internal fun TangemPayChangePinScreen(
    state: TangemPayChangePinUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AppBarWithBackButton(
            modifier = Modifier.statusBarsPadding(),
            onBackClick = onBackClick,
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .padding(horizontal = 36.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.visa_onboarding_pin_code_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )

            SpacerH16()

            Text(
                text = stringResourceSafe(R.string.visa_onboarding_pin_code_description),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )

            SpacerH(26.dp)

            PinCodeSection(state)
        }

        NavigationPrimaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            primaryButton = NavigationButton(
                textReference = resourceReference(R.string.common_submit),
                onClick = state.onSubmitClick,
                shouldShowProgress = state.submitButtonLoading,
                isEnabled = state.submitButtonEnabled,
            ),
        )
    }
}

@Composable
private fun PinCodeSection(state: TangemPayChangePinUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PinCode(
            modifier = modifier,
            value = state.pinCode,
            onValueChange = state.onPinCodeChange,
            focusRequester = focusRequester,
        )

        AnimatedVisibility(
            visible = state.error != null,
        ) {
            val error = remember(this) { requireNotNull(state.error) }
            Column {
                SpacerH4()
                Text(
                    text = error.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.warning,
                    textAlign = TextAlign.Center,
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
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numbersCount: Int = 4,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = value,
        onValueChange = { text ->
            if (text.length <= numbersCount) {
                onValueChange(text)
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        textStyle = TangemTheme.typography.h1.copy(color = Transparent),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(numbersCount) { index ->
                        val digit = value.getOrNull(index)?.toString()
                        PinDigitBox(
                            digit = digit,
                            backgroundColor = TangemTheme.colors.field.focused,
                            borderColor = TangemTheme.colors.stroke.primary,
                            textColor = TangemTheme.colors.text.primary1,
                            textStyle = TangemTheme.typography.h1,
                        )
                    }
                }
                innerTextField()
            }
        },
    )
}