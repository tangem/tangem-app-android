package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state.MultiWalletBackupUM

@Composable
fun MultiWalletBackup(state: MultiWalletBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .background(Color.Black),
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            text = stringResource(R.string.onboarding_button_add_backup_card),
            onClick = state.onAddBackupClick,
        )

        SecondaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            enabled = state.finalizeButtonEnabled,
            text = stringResource(R.string.onboarding_button_finalize_backup),
            onClick = state.onFinalizeButtonClick,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletBackup(
            state = MultiWalletBackupUM(
                finalizeButtonEnabled = true,
                onAddBackupClick = {},
                onFinalizeButtonClick = {},
            ),
        )
    }
}
