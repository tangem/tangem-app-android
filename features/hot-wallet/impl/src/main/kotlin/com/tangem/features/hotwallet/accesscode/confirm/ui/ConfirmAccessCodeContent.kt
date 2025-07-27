package com.tangem.features.hotwallet.accesscode.confirm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.hotwallet.accesscode.ui.AccessCodeEnter
import com.tangem.core.res.R

@Composable
internal fun SetAccessCodeConfirmContent(
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    accessCodeLength: Int,
    onConfirm: () -> Unit,
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
            reEnterAccessCodeState = true,
        )

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            text = stringResourceSafe(R.string.common_confirm),
            onClick = onConfirm,
            enabled = buttonEnabled,
        )
    }
}