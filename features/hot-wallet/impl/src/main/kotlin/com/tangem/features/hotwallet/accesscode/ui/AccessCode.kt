package com.tangem.features.hotwallet.accesscode.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.accesscode.entity.AccessCodeUM
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun AccessCode(
    state: AccessCodeUM,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary)
                .navigationBarsPadding(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp),
                color = TangemTheme.colors.icon.secondary,
                strokeWidth = 2.dp,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 56.dp)
                    .align(Alignment.CenterHorizontally),
                text = if (state.isConfirmMode) {
                    stringResourceSafe(R.string.access_code_confirm_title)
                } else {
                    stringResourceSafe(R.string.access_code_create_title)
                },
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                text = if (state.isConfirmMode) {
                    stringResourceSafe(R.string.access_code_confirm_description)
                } else {
                    stringResourceSafe(
                        R.string.access_code_create_description,
                        state.accessCodeLength,
                    )
                },
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PinTextField(
                    length = state.accessCodeLength,
                    isPasswordVisual = state.isConfirmMode,
                    value = state.accessCode,
                    pinTextColor = state.accessCodeColor,
                    onValueChange = state.onAccessCodeChange,
                    focusRequester = focusRequester,
                )
            }
        }
    }

    val hapticManager = LocalHapticManager.current

    LaunchedEffect(state.accessCodeColor) {
        when (state.accessCodeColor) {
            PinTextColor.WrongCode -> {
                launch(NonCancellable) {
                    delay(timeMillis = 500)
                    hapticManager.perform(TangemHapticEffect.View.Reject)
                }
            }
            else -> Unit
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSet() {
    TangemThemePreview {
        AccessCode(
            state = AccessCodeUM(
                accessCode = "",
                accessCodeColor = PinTextColor.Primary,
                onAccessCodeChange = {},
                isConfirmMode = false,
                isLoading = false,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConfirm() {
    TangemThemePreview {
        AccessCode(
            state = AccessCodeUM(
                accessCode = "123456",
                accessCodeColor = PinTextColor.Success,
                onAccessCodeChange = {},
                isConfirmMode = true,
                isLoading = false,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PreviewConfirmLoading() {
    TangemThemePreview {
        AccessCode(
            state = AccessCodeUM(
                accessCode = "123456",
                accessCodeColor = PinTextColor.Success,
                onAccessCodeChange = {},
                isConfirmMode = true,
                isLoading = true,
            ),
        )
    }
}