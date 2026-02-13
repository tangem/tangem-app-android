package com.tangem.features.onboarding.usedcard.syncwallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.onboarding.usedcard.impl.R

@Composable
internal fun SyncWalletScreen(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO [REDACTED_TASK_KEY]
        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = stringResourceSafe(R.string.common_continue),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}