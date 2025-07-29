package com.tangem.features.hotwallet.setaccesscode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton

@Suppress("LongParameterList")
@Composable
internal fun AccessCodeLayout(
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    accessCodeLength: Int,
    reEnterAccessCodeState: Boolean,
    buttonText: String,
    onButtonClick: () -> Unit,
    buttonEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AccessCodeEnter(
            modifier = Modifier
                .padding(top = 16.dp)
                .weight(1f),
            accessCode = accessCode,
            onAccessCodeChange = onAccessCodeChange,
            accessCodeLength = accessCodeLength,
            reEnterAccessCodeState = reEnterAccessCodeState,
        )

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            text = buttonText,
            onClick = onButtonClick,
            enabled = buttonEnabled,
        )
    }
}