package com.tangem.features.hotwallet.setaccesscode.ui

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
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.setaccesscode.entity.SetAccessCodeUM

@Composable
internal fun SetAccessCodeEnter(
    state: SetAccessCodeUM,
    reEnterAccessCodeState: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .padding(top = 56.dp)
                .align(Alignment.CenterHorizontally),
            text = if (reEnterAccessCodeState) {
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
            text = if (reEnterAccessCodeState) {
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
                value = if (reEnterAccessCodeState) {
                    state.accessCodeSecond
                } else {
                    state.accessCodeFirst
                },
                onValueChange = if (reEnterAccessCodeState) {
                    state.onAccessCodeSecondChange
                } else {
                    state.onAccessCodeFirstChange
                },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        SetAccessCodeEnter(
            reEnterAccessCodeState = false,
            state = SetAccessCodeUM(
                step = SetAccessCodeUM.Step.AccessCode,
                accessCodeFirst = "",
                accessCodeSecond = "",
                onAccessCodeFirstChange = {},
                onAccessCodeSecondChange = {},
                buttonEnabled = false,
                onContinue = {},
            ),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview2() {
    TangemThemePreview {
        SetAccessCodeEnter(
            reEnterAccessCodeState = true,
            state = SetAccessCodeUM(
                step = SetAccessCodeUM.Step.ConfirmAccessCode,
                accessCodeFirst = "",
                accessCodeSecond = "",
                onAccessCodeFirstChange = {},
                onAccessCodeSecondChange = {},
                buttonEnabled = false,
                onContinue = {},
            ),
        )
    }
}