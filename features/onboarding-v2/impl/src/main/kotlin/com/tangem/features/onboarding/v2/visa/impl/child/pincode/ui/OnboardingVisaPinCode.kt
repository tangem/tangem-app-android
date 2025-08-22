package com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state.OnboardingVisaPinCodeUM
import kotlinx.coroutines.delay

@Composable
internal fun OnboardingVisaPinCode(state: OnboardingVisaPinCodeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
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
                showProgress = state.submitButtonLoading,
                isEnabled = state.submitButtonEnabled,
            ),
        )
    }
}

@Composable
private fun PinCodeSection(state: OnboardingVisaPinCodeUM, modifier: Modifier = Modifier) {
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
    onDone: ((String) -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { text ->
                if (text.length <= numbersCount) {
                    onValueChange(text)
                    if (text.length == numbersCount) {
                        onDone?.invoke(text)
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                },
            ),
            modifier = Modifier
                .alpha(alpha = 0.01f)
                .focusRequester(focusRequester),
            textStyle = TextStyle.Default.copy(color = Transparent),
        )

        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        keyboardController?.show()
                        focusRequester.requestFocus()
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(numbersCount) { index ->
                Box(
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors.field.primary,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        )
                        .padding(
                            vertical = 12.dp,
                            horizontal = 16.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.widthIn(min = measureTextWidth("0", TangemTheme.typography.h1)),
                        text = value.getOrNull(index)?.toString() ?: "",
                        style = TangemTheme.typography.h1,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
            }
        }
    }
}

@Composable
private fun measureTextWidth(text: String, style: TextStyle): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaPinCode(
            state = OnboardingVisaPinCodeUM(
                pinCode = "1234",
                error = stringReference("PIN Code error"),
            ),
        )
    }
}