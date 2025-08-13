package com.tangem.features.hotwallet.accesscode.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.accesscode.entity.AccessCodeUM

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun AccessCode(state: AccessCodeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .weight(1f)
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary)
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
                    isPasswordVisual = true,
                    value = state.accessCode,
                    onValueChange = state.onAccessCodeChange,
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            text = stringResourceSafe(
                if (state.isConfirmMode) {
                    R.string.common_confirm
                } else {
                    R.string.common_continue
                },
            ),
            onClick = state.onButtonClick,
            enabled = state.buttonEnabled,
            showProgress = state.buttonInProgress,
        )
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
                onAccessCodeChange = {},
                isConfirmMode = false,
                buttonEnabled = false,
                buttonInProgress = false,
                onButtonClick = {},
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
                onAccessCodeChange = {},
                isConfirmMode = true,
                buttonEnabled = true,
                buttonInProgress = false,
                onButtonClick = {},
            ),
        )
    }
}