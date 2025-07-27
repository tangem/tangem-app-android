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
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun AccessCodeEnter(
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    accessCodeLength: Int,
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
                    accessCodeLength,
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
                length = accessCodeLength,
                isPasswordVisual = true,
                value = accessCode,
                onValueChange = onAccessCodeChange,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        AccessCodeEnter(
            accessCode = "123456",
            onAccessCodeChange = {},
            accessCodeLength = 6,
            reEnterAccessCodeState = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview2() {
    TangemThemePreview {
        AccessCodeEnter(
            accessCode = "123456",
            onAccessCodeChange = {},
            accessCodeLength = 6,
            reEnterAccessCodeState = true,
        )
    }
}