package com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.ui.state.MultiWalletUpgradeWalletUM

@Composable
internal fun MultiWalletUpgradeWallet(state: MultiWalletUpgradeWalletUM, modifier: Modifier = Modifier) {
    if (state.dialog != null) {
        BasicDialog(
            title = state.dialog.title.resolveReference(),
            message = state.dialog.message.resolveReference(),
            confirmButton = DialogButtonUM(
                title = state.dialog.confirmButtonText.resolveReference(),
                onClick = state.dialog.onConfirmClick,
            ),
            dismissButton = DialogButtonUM(
                title = state.dialog.dismissButtonText.resolveReference(),
                warning = state.dialog.dismissWarningColor,
                onClick = state.dialog.onDismissButtonClick,
            ),
            onDismissDialog = state.dialog.onDismiss,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = state.bodyText.resolveReference(),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            text = stringResourceSafe(R.string.hw_upgrade_start_action),
            onClick = state.onStartUpgradeClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletUpgradeWallet(
            state = MultiWalletUpgradeWalletUM(
                title = stringReference("Title"),
                bodyText = stringReference("Body body body"),
                onStartUpgradeClick = {},
                dialog = null,
            ),
        )
    }
}