package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui

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
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun Wallet1ChooseOption(
    canSkipBackup: Boolean,
    onSkipClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // val pagerState = rememberPagerState(pageCount = { 10 }) // TODO [REDACTED_TASK_KEY]

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.onboarding_wallet_info_title_first),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = stringResourceSafe(R.string.onboarding_wallet_info_subtitle_first),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_button_backup_now),
            onClick = onBackupClick,
        )

        if (canSkipBackup) {
            SecondaryButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.onboarding_button_skip_backup),
                onClick = onSkipClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        Wallet1ChooseOption(
            canSkipBackup = true,
            onSkipClick = {},
            onBackupClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}